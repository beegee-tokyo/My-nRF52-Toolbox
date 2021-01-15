package tk.giesecke.my_nrf52_tb.lora_setup;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import no.nordicsemi.android.ble.data.Data;
import tk.giesecke.my_nrf52_tb.lora_setup.callback.LoRa_Settings_ManagerCallbacks;
import tk.giesecke.my_nrf52_tb.profile.BleManager;

import static java.lang.Math.abs;
import static tk.giesecke.my_nrf52_tb.lora_setup.LoRaSettingsService.BROADCAST_DATA_RECVD;
import static tk.giesecke.my_nrf52_tb.lora_setup.LoRaSettingsService.EXTRA_DATA;
import static tk.giesecke.my_nrf52_tb.profile.BleProfileService.EXTRA_DEVICE;

public class LoRa_Settings_Manager extends BleManager<LoRa_Settings_ManagerCallbacks> {

    static String TAG = "LR_SETT";
    /**
     * The LoRa service UUID.
     */
    static final UUID LORA_SERVICE_UUID = UUID.fromString("0000f0a0-ead2-11e7-80c1-9a214cf093ae"); // LoRa service ID
//    static final UUID LORA_SERVICE_UUID = UUID.fromString("f0a0"); // LoRa service ID
    /**
     * The LoRa settings UUID. 0000aaaa-ead2-11e7-80c1-9a214cf093ae
     */
    private static final UUID LORA_SETTINGS_UUID = UUID.fromString("0000f0a1-ead2-11e7-80c1-9a214cf093ae"); // LoRa settings
//    private static final UUID LORA_SETTINGS_UUID = UUID.fromString("f0a1"); // LoRa settings

    private BluetoothGattCharacteristic mRequiredCharacteristic, mSettingsCharacteristic;

    LoRa_Settings_Manager(final Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return mGattCallback;
    }

    /**
     * BluetoothGatt callbacks for connection/disconnection, service discovery,
     * receiving indication, etc.
     */
    private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {

        @Override
        protected void initialize() {
            // Initialize the Battery Manager. It will enable Battery Level notifications.
            // Remove it if you don't need this feature.
            super.initialize();

            // Increase the MTU
            requestMtu(200)
                    .with((device, mtu) -> {
                        Log.d(TAG, "MTU changed to " + mtu);
                    })
                    .done(device -> {
                        // You may do some logic in here that should be done when the request finished successfully.
                        // In case of MTU this method is called also when the MTU hasn't changed, or has changed
                        // to a different (lower) value. Use .with(...) to get the MTU value.
                        Log.d(TAG, "MTU size changed");
                    })
                    .fail((device, status) -> Log.d(TAG, "MTU size change failed"))
                    .enqueue();
        }

        @Override
        protected boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            // It should return true if all has been discovered (that is that device is supported).
            final BluetoothGattService service = gatt.getService(LORA_SERVICE_UUID);
            if (service != null) {
                mRequiredCharacteristic = service.getCharacteristic(LORA_SETTINGS_UUID);
                mSettingsCharacteristic = service.getCharacteristic(LORA_SETTINGS_UUID);
            }
            return mRequiredCharacteristic != null;
        }

        @Override
        protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
            // Initialize Settings characteristic
            super.isOptionalServiceSupported(gatt);

            return true;
        }

        @Override
        protected void onDeviceDisconnected() {
            // Release the characteristics
            mRequiredCharacteristic = null;
            mSettingsCharacteristic = null;
        }

        @Override
        protected void onDeviceReady() {
            super.onDeviceReady();

            // Initialization is now ready.
            // The service or activity has been notified with CollectorManagerCallbacks#onDeviceReady().

            readLoRaSettings();
        }
    };

    // TODO Define manager's API

    /**
     * This method will write important data to the device.
     *
     * @param parameter parameter to be written.
     */
    void resetNode(final String parameter) {
        // Write some data to the characteristic.
        writeCharacteristic(mSettingsCharacteristic, Data.from(parameter))
                // If data are longer than MTU-3, they will be chunked into multiple packets.
                // Check out other split options, with .split(...).
                .split()
                // Callback called when data were sent, or added to outgoing queue in case
                // Write Without Request type was used.
                .with((device, data) -> {
                    Log.d(TAG, data.size() + " bytes were sent");
                })
                // Callback called when data were sent, or added to outgoing queue in case
                // Write Without Request type was used. This is called after .with(...) callback.
                .done(device -> {
                    Log.d(TAG, "Reset Request sent");
                })
                // Callback called when write has failed.
                .fail((device, status) -> {
                    Log.d(TAG, "Failed to send Reset Request");
                })
                .enqueue();
    }

    /**
     * This method will write important data to the device.
     *
     * @param parameter parameter to be written.
     */
    void writeLoRaSettings(final String parameter) {
        // Write some data to the characteristic.
        writeCharacteristic(mSettingsCharacteristic, Data.from(parameter))
                // If data are longer than MTU-3, they will be chunked into multiple packets.
                // Check out other split options, with .split(...).
                .split()
                // Callback called when data were sent, or added to outgoing queue in case
                // Write Without Request type was used.
                .with((device, data) -> {
                    Log.d(TAG, data.size() + " bytes were sent");
                })
                // Callback called when data were sent, or added to outgoing queue in case
                // Write Without Request type was used. This is called after .with(...) callback.
                .done(device -> {
                    Log.d(TAG, "Settings sent");
                })
                // Callback called when write has failed.
                .fail((device, status) -> {
                    Log.e(TAG, "Failed to send settings");
                })
                .enqueue();
    }

    /**
     * This method will write important data to the device.
     *
     * @param parameter parameter to be written.
     */
    void writeLoRaSettings(final Data parameter) {
        // Write some data to the characteristic.
        writeCharacteristic(mSettingsCharacteristic, parameter)
                // If data are longer than MTU-3, they will be chunked into multiple packets.
                // Check out other split options, with .split(...).
                .split()
                // Callback called when data were sent, or added to outgoing queue in case
                // Write Without Request type was used.
                .with((device, data) -> {
                    Log.d(TAG, data.size() + " bytes were sent");
                })
                // Callback called when data were sent, or added to outgoing queue in case
                // Write Without Request type was used. This is called after .with(...) callback.
                .done(device -> {
                    Log.d(TAG, "Settings sent");
                })
                // Callback called when write has failed.
                .fail((device, status) -> {
                    Log.e(TAG, "Failed to send settings");
                })
                .enqueue();
    }

    /**
     * This method will read data from the device.
     */
    void readLoRaSettings() {
        readCharacteristic(mSettingsCharacteristic)
                .with((device, data) -> {
                    // Characteristic value has been read
                    // Let's do some magic with it.
                    LoRa_Settings_Activity.mmDevice = device.getName();
                    byte[] deviceData = data.getValue();
                    int len = data.getValue().length;
                    if (deviceData == null) {
                        return;
                    }
                    if (deviceData[0] != -86) { // -86 ==> 0xAA
                        Log.e("TAG", "Invalid data");
                    }
                    if (deviceData[1] != 0x55) {
                        Log.e("TAG", "Invalid data");
                    } else {
                        LoRa_Settings_Activity.nodeDeviceEUI = String.format("%02X%02X%02X%02X%02X%02X%02X%02X",
                                deviceData[2], deviceData[3], deviceData[4], deviceData[5],
                                deviceData[6], deviceData[7], deviceData[8], deviceData[9]);
                        LoRa_Settings_Activity.nodeAppEUI = String.format("%02X%02X%02X%02X%02X%02X%02X%02X",
                                deviceData[10], deviceData[11], deviceData[12], deviceData[13],
                                deviceData[14], deviceData[15], deviceData[16], deviceData[17]);
                        LoRa_Settings_Activity.nodeAppKey = String.format("%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X",
                                deviceData[18], deviceData[19], deviceData[20], deviceData[21],
                                deviceData[22], deviceData[23], deviceData[24], deviceData[25],
                                deviceData[26], deviceData[27], deviceData[28], deviceData[29],
                                deviceData[30], deviceData[31], deviceData[32], deviceData[33]);
                        LoRa_Settings_Activity.nodeDeviceAddr = String.format("%02X%02X%02X%02X",
                                deviceData[39], deviceData[38], deviceData[37], deviceData[36]);
                        LoRa_Settings_Activity.nodeNwsKey = String.format("%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X",
                                deviceData[40], deviceData[41], deviceData[42], deviceData[43],
                                deviceData[44], deviceData[45], deviceData[46], deviceData[47],
                                deviceData[48], deviceData[49], deviceData[50], deviceData[51],
                                deviceData[52], deviceData[53], deviceData[54], deviceData[55]);
                        LoRa_Settings_Activity.nodeAppsKey = String.format("%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X",
                                deviceData[56], deviceData[57], deviceData[58], deviceData[59],
                                deviceData[60], deviceData[61], deviceData[62], deviceData[63],
                                deviceData[64], deviceData[65], deviceData[66], deviceData[67],
                                deviceData[68], deviceData[69], deviceData[70], deviceData[71]);

                        LoRa_Settings_Activity.otaaEna = (deviceData[72] == 1);
                        LoRa_Settings_Activity.adrEna = (deviceData[73] == 1);
                        LoRa_Settings_Activity.publicNetwork = (deviceData[74] == 1);
                        LoRa_Settings_Activity.dutyCycleEna = (deviceData[75] == 1);

                        String value = String.format("%02X%02X%02X%02X",
                                deviceData[79], deviceData[78], deviceData[77], deviceData[76]);
                        LoRa_Settings_Activity.sendRepeatTime = Long.parseLong(value, 16);
//                        LoRa_Settings_Activity.sendRepeatTime = (deviceData[79] << 24) + (deviceData[78] << 16) + (deviceData[77] << 8) + (deviceData[76]);
                        LoRa_Settings_Activity.nbTrials = (byte) abs(deviceData[80]);
                        LoRa_Settings_Activity.txPower = (byte) abs(deviceData[81]);
                        LoRa_Settings_Activity.dataRate = (byte) abs(deviceData[82]);
                        LoRa_Settings_Activity.loraClass = (byte) abs(deviceData[83]);
                        LoRa_Settings_Activity.subBandChannel = (byte) abs(deviceData[84]);

                        LoRa_Settings_Activity.autoJoin = (deviceData[85] == 1);

                        LoRa_Settings_Activity.appPort = (byte) abs(deviceData[86]);

                        LoRa_Settings_Activity.confirmedEna = (deviceData[87] == 1);

                        LoRa_Settings_Activity.region = (byte) abs(deviceData[88]);

                        LoRa_Settings_Activity.loraWanEna = (deviceData[89] == 1);

                        value = String.format("%02X%02X%02X%02X",
                                deviceData[95], deviceData[94], deviceData[93], deviceData[92]);
                        LoRa_Settings_Activity.p2pFrequency = Long.parseLong(value, 16);

                        LoRa_Settings_Activity.p2pTxPower = (byte) abs(deviceData[96]);
                        LoRa_Settings_Activity.p2pBW = (byte) abs(deviceData[97]);
                        LoRa_Settings_Activity.p2pSF = (byte) abs(deviceData[98]);
                        LoRa_Settings_Activity.p2pCR = (byte) abs(deviceData[99]);
                        LoRa_Settings_Activity.p2pPreLen = (byte) abs(deviceData[100]);

                        value = String.format("%02X%02X",
                                deviceData[103], deviceData[102]);
                        LoRa_Settings_Activity.p2pSymTimeout = Integer.parseInt(value, 16);

                        final Intent broadcast = new Intent(BROADCAST_DATA_RECVD);
                        broadcast.putExtra(EXTRA_DEVICE, getBluetoothDevice());
                        broadcast.putExtra(EXTRA_DATA, data.getValue());
                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(broadcast);
                    }
                })
                .enqueue();
    }
}