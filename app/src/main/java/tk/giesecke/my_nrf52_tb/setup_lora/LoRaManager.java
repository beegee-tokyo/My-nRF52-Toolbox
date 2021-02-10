/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tk.giesecke.my_nrf52_tb.setup_lora;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.Intent;
import android.util.Log;

import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.data.Data;
import tk.giesecke.my_nrf52_tb.battery.BatteryManager;
import tk.giesecke.my_nrf52_tb.setup_lora.callback.LoRaDataCallback;

import static java.lang.Math.abs;
import static tk.giesecke.my_nrf52_tb.profile.BleProfileService.EXTRA_DEVICE;
import static tk.giesecke.my_nrf52_tb.setup_lora.LoRaService.BROADCAST_DATA_RECVD;
import static tk.giesecke.my_nrf52_tb.setup_lora.LoRaService.EXTRA_DATA;

/**
 * Modify to template manager to match your requirements.
 * The LoRaManager extends {@link BleManager}.
 */
public class LoRaManager extends BatteryManager<LoRaManagerCallbacks> {

    static final String TAG = "LR_SETT";
    /**
     * The LoRa service UUID.
     */
    static final UUID LORA_SERVICE_UUID = UUID.fromString("0000f0a0-0000-1000-8000-00805f9b34fb"); // LoRa service ID
    /**
     * The LoRa settings UUID. 0000aaaa-ead2-11e7-80c1-9a214cf093ae
     */
    static final UUID LORA_SETTINGS_UUID = UUID.fromString("0000f0a1-0000-1000-8000-00805f9b34fb"); // LoRa settings

    static BluetoothGattCharacteristic requiredCharacteristic, settingsCharacteristic;

    public LoRaManager(final Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected BatteryManagerGattCallback getGattCallback() {
        return new BleManagerGattCallback();
    }

    /**
     * BluetoothGatt callbacks for connection/disconnection, service discovery,
     * receiving indication, etc.
     */
    private class BleManagerGattCallback extends BatteryManagerGattCallback {

        @Override
        protected void initialize() {
            // Initialize the Battery Manager. It will enable Battery Level notifications.
            // Remove it if you don't need this feature.
            super.initialize();

            // Increase the MTU
            requestMtu(200)
                    .with((device, mtu) -> Log.d(TAG, "MTU changed to " + mtu))
                    .done(device -> Log.d(TAG, "MTU size changed"))
                    .fail((device, status) -> Log.d(TAG, "MTU size change failed"))
                    .enqueue();
        }

        @Override
        protected boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            // It should return true if all has been discovered (that is that device is supported).
            final BluetoothGattService service = gatt.getService(LORA_SERVICE_UUID);
            if (service != null) {
                requiredCharacteristic = service.getCharacteristic(LORA_SETTINGS_UUID);
                settingsCharacteristic = service.getCharacteristic(LORA_SETTINGS_UUID);
            }
            return requiredCharacteristic != null;
        }

        @Override
        protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
            // Initialize Settings characteristic
            super.isOptionalServiceSupported(gatt);

            return true;
        }

        @Override
        protected void onDeviceDisconnected() {
            // Release references to your characteristics.
            requiredCharacteristic = null;
            settingsCharacteristic = null;
        }

        @Override
        protected void onDeviceReady() {
            super.onDeviceReady();

            // Initialization is now ready.
            // The service or activity has been notified with LoRaManagerCallbacks#onDeviceReady().
            readLoRaSettings();
        }
    }

    // Manager's API

    /**
     * This method will write important data to the device.
     *
     * @param parameter parameter to be written.
     */
    void writeLoRaSettings(final Data parameter) {
        // Write some data to the characteristic.
        writeCharacteristic(settingsCharacteristic, parameter)
                // If data are longer than MTU-3, they will be chunked into multiple packets.
                // Check out other split options, with .split(...).
                .split()
                // Callback called when data were sent, or added to outgoing queue in case
                // Write Without Request type was used.
                .with((device, data) -> Log.d(TAG, data.size() + " bytes were sent"))
                // Callback called when data were sent, or added to outgoing queue in case
                // Write Without Request type was used. This is called after .with(...) callback.
                .done(device -> Log.d(TAG, "Settings sent"))
                // Callback called when write has failed.
                .fail((device, status) -> Log.e(TAG, "Failed to send settings"))
                .enqueue();
    }

    /**
     * This method will read data from the device.
     */
    void readLoRaSettings() {
        readCharacteristic(settingsCharacteristic)
                .with((device, data) -> {
                    // Characteristic value has been read
                    // Parse the data and check for validity
                    LoRaActivity.mmDevice = device.getName();
                    byte[] deviceData = data.getValue();

                    if (deviceData == null) {
                        return;
                    }
                    if (deviceData[0] != -86) { // -86 ==> 0xAA
                        Log.e("TAG", "Invalid data");
                    }
                    if (deviceData[1] != 0x55) {
                        Log.e("TAG", "Invalid data");
                    } else {
                        LoRaActivity.nodeDeviceEUI = String.format("%02X%02X%02X%02X%02X%02X%02X%02X",
                                deviceData[2], deviceData[3], deviceData[4], deviceData[5],
                                deviceData[6], deviceData[7], deviceData[8], deviceData[9]);
                        LoRaActivity.nodeAppEUI = String.format("%02X%02X%02X%02X%02X%02X%02X%02X",
                                deviceData[10], deviceData[11], deviceData[12], deviceData[13],
                                deviceData[14], deviceData[15], deviceData[16], deviceData[17]);
                        LoRaActivity.nodeAppKey = String.format("%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X",
                                deviceData[18], deviceData[19], deviceData[20], deviceData[21],
                                deviceData[22], deviceData[23], deviceData[24], deviceData[25],
                                deviceData[26], deviceData[27], deviceData[28], deviceData[29],
                                deviceData[30], deviceData[31], deviceData[32], deviceData[33]);
                        LoRaActivity.nodeDeviceAddr = String.format("%02X%02X%02X%02X",
                                deviceData[39], deviceData[38], deviceData[37], deviceData[36]);
                        LoRaActivity.nodeNwsKey = String.format("%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X",
                                deviceData[40], deviceData[41], deviceData[42], deviceData[43],
                                deviceData[44], deviceData[45], deviceData[46], deviceData[47],
                                deviceData[48], deviceData[49], deviceData[50], deviceData[51],
                                deviceData[52], deviceData[53], deviceData[54], deviceData[55]);
                        LoRaActivity.nodeAppsKey = String.format("%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X",
                                deviceData[56], deviceData[57], deviceData[58], deviceData[59],
                                deviceData[60], deviceData[61], deviceData[62], deviceData[63],
                                deviceData[64], deviceData[65], deviceData[66], deviceData[67],
                                deviceData[68], deviceData[69], deviceData[70], deviceData[71]);

                        if (deviceData[72] > 1) {
                            LoRaActivity.otaaEna = false;
                        } else {
                            LoRaActivity.otaaEna = (deviceData[72] == 1);
                        }
                        if (deviceData[73] > 1) {
                            LoRaActivity.adrEna = false;
                        } else {
                            LoRaActivity.adrEna = (deviceData[73] == 1);
                        }
                        if (deviceData[74] > 1) {
                            LoRaActivity.publicNetwork = false;
                        } else {
                            LoRaActivity.publicNetwork = (deviceData[74] == 1);
                        }
                        if (deviceData[75] > 1) {
                            LoRaActivity.publicNetwork = true;
                        } else {
                            LoRaActivity.dutyCycleEna = (deviceData[75] == 1);
                        }

                        String value = String.format("%02X%02X%02X%02X",
                                deviceData[79], deviceData[78], deviceData[77], deviceData[76]);
                        if (((Long.parseLong(value, 16)) > 3600000) || ((Long.parseLong(value, 16)) < 10000)) {
                            LoRaActivity.sendRepeatTime = 120000;
                        } else {
                            LoRaActivity.sendRepeatTime = Long.parseLong(value, 16);
                        }
                        if (abs(deviceData[80]) > 12) {
                            LoRaActivity.nbTrials = 5;
                        } else {
                            LoRaActivity.nbTrials = (byte) abs(deviceData[80]);
                        }
                        if (abs(deviceData[81]) > 15) {
                            LoRaActivity.txPower = 15;
                        } else {
                            LoRaActivity.txPower = (byte) abs(deviceData[81]);
                        }
                        if (abs(deviceData[82]) > 15) {
                            LoRaActivity.dataRate = 3;
                        } else {
                            LoRaActivity.dataRate = (byte) abs(deviceData[82]);
                        }
                        if (abs(deviceData[83]) > 2) {
                            LoRaActivity.loraClass = 0;
                        } else {
                            LoRaActivity.loraClass = (byte) abs(deviceData[83]);
                        }
                        if (abs(deviceData[84]) > 9) {
                            LoRaActivity.subBandChannel = 1;
                        } else {
                            LoRaActivity.subBandChannel = (byte) abs(deviceData[84]);
                        }

                        if (abs(deviceData[85]) > 1) {
                            LoRaActivity.autoJoin = false;
                        } else {
                            LoRaActivity.autoJoin = (deviceData[85] == 1);
                        }

                        if (abs(deviceData[86]) > 127) {
                            LoRaActivity.appPort = 2;
                        } else {
                            LoRaActivity.appPort = (byte) abs(deviceData[86]);
                        }

                        if (abs(deviceData[87]) > 1) {
                            LoRaActivity.confirmedEna = false;
                        } else {
                            LoRaActivity.confirmedEna = (deviceData[87] == 1);
                        }

                        if (abs(deviceData[88]) > 9) {
                            LoRaActivity.region = 4;
                        } else {
                            LoRaActivity.region = (byte) abs(deviceData[88]);
                        }

                        if (abs(deviceData[89]) > 1) {
                            LoRaActivity.loraWanEna = false;
                        } else {
                            LoRaActivity.loraWanEna = (deviceData[89] == 1);
                        }

                        value = String.format("%02X%02X%02X%02X",
                                deviceData[95], deviceData[94], deviceData[93], deviceData[92]);
                        if ((Long.parseLong(value, 16) < 150000000) || (Long.parseLong(value, 16) > 960000000)) {
                            LoRaActivity.p2pFrequency = 923300000;
                        } else {
                            LoRaActivity.p2pFrequency = Long.parseLong(value, 16);
                        }

                        if (abs(deviceData[96]) > 22) {
                            LoRaActivity.p2pTxPower = 22;
                        } else {
                            LoRaActivity.p2pTxPower = (byte) abs(deviceData[96]);
                        }
                        if (abs(deviceData[97]) > 2) {
                            LoRaActivity.p2pBW = 0;
                        } else {
                            LoRaActivity.p2pBW = (byte) abs(deviceData[97]);
                        }
                        if ((abs(deviceData[98]) < 7) || (abs(deviceData[98]) > 12)) {
                            LoRaActivity.p2pSF = 7;
                        } else {
                            LoRaActivity.p2pSF = (byte) abs(deviceData[98]);
                        }
                        if ((abs(deviceData[99]) < 5) || (abs(deviceData[99]) > 8)) {
                            LoRaActivity.p2pCR = 5;
                        } else {
                            LoRaActivity.p2pCR = (byte) abs(deviceData[99]);
                        }
                        if ((abs(deviceData[100]) < 1) || (abs(deviceData[100]) > 16)) {
                            LoRaActivity.p2pPreLen = 8;
                        } else {
                            LoRaActivity.p2pPreLen = (byte) abs(deviceData[100]);
                        }

                        value = String.format("%02X%02X",
                                deviceData[103], deviceData[102]);
                        LoRaActivity.p2pSymTimeout = Integer.parseInt(value, 16);

                        final Intent broadcast = new Intent(BROADCAST_DATA_RECVD);
                        broadcast.putExtra(EXTRA_DEVICE, getBluetoothDevice());
                        broadcast.putExtra(EXTRA_DATA, data.getValue());
                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(broadcast);
                    }
                })
                .enqueue();
    }

    void requestNotification(BluetoothGattCharacteristic gattChar) {
        if (gattChar == requiredCharacteristic) {
            setNotificationCallback(requiredCharacteristic)
                    // This callback will be called each time the notification is received
                    .with(new LoRaDataCallback() {
                        @Override
                        public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
                            // Let's pass received data to the service
                            mCallbacks.onSampleValueReceived(device, data);
                        }

                        @Override
                        public void onSampleValueReceived(@NonNull final BluetoothDevice device, final Data data) {
                            // Let's pass received data to the service
                            mCallbacks.onSampleValueReceived(device, data);
                        }

                        @Override
                        public void onInvalidDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
                            Log.e(TAG, "Invalid data received: " + data);
                        }
                    });
            enableNotifications(requiredCharacteristic)
                    // Method called after the data were sent (data will contain 0x0100 in this case)
                    .with((device, data) -> {
                    })
                    // Method called when the request finished successfully. This will be called after .with(..) callback
                    .done(device -> Log.d(TAG, "LoRa Settings Notifications enabled"))
                    // Methods called in case of an error, for example when the characteristic does not have Notify property
                    .fail((device, status) -> Log.e(TAG, "LoRa Settings Notifications failed with " + status))
                    .enqueue();
        }
    }

    void stopNotification(BluetoothGattCharacteristic gattChar) {
        disableNotifications(gattChar)
                // Method called after the data were sent (data will contain 0x0100 in this case)
                .with((device, data) -> {
                })
                // Method called when the request finished successfully. This will be called after .with(..) callback
                .done(device -> Log.d(TAG, "Notifications disabled"))
                // Methods called in case of an error, for example when the characteristic does not have Notify property
                .fail((device, status) -> Log.d(TAG, "Disable notifications failed with " + status))
                .enqueue();
    }
}
