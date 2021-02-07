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
package tk.giesecke.my_nrf52_tb.setup_esp32;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.data.Data;
import tk.giesecke.my_nrf52_tb.battery.BatteryManager;
import tk.giesecke.my_nrf52_tb.setup_esp32.callback.ESP32DataCallback;

import static tk.giesecke.my_nrf52_tb.profile.BleProfileService.EXTRA_DEVICE;
import static tk.giesecke.my_nrf52_tb.setup_esp32.ESP32Service.BROADCAST_DATA_RECVD;
import static tk.giesecke.my_nrf52_tb.setup_esp32.ESP32Service.EXTRA_DATA;
import static tk.giesecke.my_nrf52_tb.setup_esp32.XorCoding.xorCode;

/**
 * Modify to template manager to match your requirements.
 * The ESP32Manager extends {@link BleManager}.
 */
public class ESP32Manager extends BatteryManager<ESP32ManagerCallbacks> {

	static String TAG = "ESP32_MAN";
	/**
	 * The service UUID.
	 */
	static final UUID ESP32_SERVICE_UUID = UUID.fromString("0000aaaa-ead2-11e7-80c1-9a214cf093ae"); // ESP32 service
	/**
	 * A UUID of a characteristic with notify property.
	 */
	private static final UUID WIFI_SETTINGS_UUID = UUID.fromString("00005555-ead2-11e7-80c1-9a214cf093ae"); // WiFi Settings characteristic

	// TODO Add more services and characteristics references.
	private BluetoothGattCharacteristic requiredCharacteristic, wifiCharacteristic;

	public ESP32Manager(final Context context) {
		super(context);
	}

	@NonNull
	@Override
	protected BatteryManagerGattCallback getGattCallback() {
		return new ESP32ManagerGattCallback();
	}

	/**
	 * BluetoothGatt callbacks for connection/disconnection, service discovery,
	 * receiving indication, etc.
	 */
	private class ESP32ManagerGattCallback extends BatteryManagerGattCallback {

		@Override
		protected void initialize() {
			// Initialize the Battery Manager. It will enable Battery Level notifications.
			// Remove it if you don't need this feature.
			super.initialize();

			// TODO Initialize your manager here.
			// Initialization is done once, after the device is connected. Usually it should
			// enable notifications or indications on some characteristics, write some data or
			// read some features / version.
			// After the initialization is complete, the onDeviceReady(...) method will be called.

			// Increase the MTU
			requestMtu(128)
					.with((device, mtu) -> {Log.d(TAG, "MTU request change to " + mtu);})
					.done(device -> {
						Log.d(TAG, "MTU size changed");
					})
					.fail((device, status) -> Log.d(TAG, "MTU change not supported"))
					.enqueue();

			// Set notification callback
			setNotificationCallback(requiredCharacteristic)
					// This callback will be called each time the notification is received
					.with(new ESP32DataCallback() {
						@Override
						public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
							Log.d(TAG, "onDataReceived");
							super.onDataReceived(device, data);
						}

						@Override
						public void onSampleValueReceived(@NonNull final BluetoothDevice device, final Data value) {
							Log.d(TAG, "onSampleValueReceived");
							// Let's lass received data to the service
							mCallbacks.onSampleValueReceived(device, value);
						}

						@Override
						public void onInvalidDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
							Log.d(TAG, "onInvalidDataReceived");
						}
					});

			// Enable notifications
			enableNotifications(requiredCharacteristic)
					// Method called after the data were sent (data will contain 0x0100 in this case)
					.with((device, data) -> Log.d(TAG, "Data sent: " + data))
					// Method called when the request finished successfully. This will be called after .with(..) callback
					.done(device -> Log.d(TAG, "Notifications enabled successfully"))
					// Methods called in case of an error, for example when the characteristic does not have Notify property
					.fail((device, status) -> Log.d(TAG, "Failed to enable notifications"))
					.enqueue();
		}

		@Override
		protected boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
			// TODO Initialize required characteristics.
			// It should return true if all has been discovered (that is that device is supported).
			final BluetoothGattService service = gatt.getService(ESP32_SERVICE_UUID);
			if (service != null) {
				requiredCharacteristic = service.getCharacteristic(WIFI_SETTINGS_UUID);
				wifiCharacteristic = service.getCharacteristic(WIFI_SETTINGS_UUID);
			}
			return requiredCharacteristic != null;
		}

		@Override
		protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
			// Initialize Battery characteristic
			super.isOptionalServiceSupported(gatt);
			return true;
		}

		@Override
		protected void onDeviceDisconnected() {
			// Release references to your characteristics.
			requiredCharacteristic = null;
			wifiCharacteristic = null;
		}

		@Override
		protected void onDeviceReady() {
			super.onDeviceReady();

			// Read and parse the received data
			readSettings();
		}
	}

	// Manager's API

	/**
	 * This method will write important data to the device.
	 *
	 * @param parameter parameter to be written.
	 */
	void writeSettings(final String parameter) {
		// Write some data to the characteristic.
		writeCharacteristic(wifiCharacteristic, Data.from(parameter))
				// If data are longer than MTU-3, they will be chunked into multiple packets.
				// Check out other split options, with .split(...).
				.split()
				// Callback called when data were sent, or added to outgoing queue in case
				// Write Without Request type was used.
				.with((device, data) -> {
					//log(Log.DEBUG, data.size() + " bytes were sent")
				})
				// Callback called when data were sent, or added to outgoing queue in case
				// Write Without Request type was used. This is called after .with(...) callback.
				.done(device -> {
					//log(LogContract.Log.Level.APPLICATION, "Settings sent")
				})
				// Callback called when write has failed.
				.fail((device, status) -> {
					//log(Log.WARN, "Failed to send settings")
				})
				.enqueue();
	}

	/**
	 * This method will read data from the device.
	 *
	 */
	void readSettings() {
		readCharacteristic(wifiCharacteristic)
				.with((device, data) -> {
					// Characteristic value has been read
					// Let's do some magic with it.
					ESP32Activity.mmDevice = device.getName();
					// Decode the data
					byte[] decodedData = xorCode(device.getName(),data.getValue(),data.size());
					String finalData = new String(decodedData);

					// Get stored WiFi credentials from the received data
					JSONObject receivedConfigJSON;
					try {
						receivedConfigJSON = new JSONObject(finalData);
						if (receivedConfigJSON.has("ssidPrim")) {
							ESP32Activity.ssidPrimString = receivedConfigJSON.getString("ssidPrim");
						}
						if (receivedConfigJSON.has("pwPrim")) {
							ESP32Activity.pwPrimString = receivedConfigJSON.getString("pwPrim");
						}
						if (receivedConfigJSON.has("ssidSec")) {
							ESP32Activity.ssidSecString = receivedConfigJSON.getString("ssidSec");
						}
						if (receivedConfigJSON.has("pwSec")) {
							ESP32Activity.pwSecString = receivedConfigJSON.getString("pwSec");
						}
						if (receivedConfigJSON.has("lora")) {
							ESP32Activity.regionSelected = (byte) receivedConfigJSON.getInt("lora");
						}
						final Intent broadcast = new Intent(BROADCAST_DATA_RECVD);
						broadcast.putExtra(EXTRA_DEVICE, getBluetoothDevice());
						broadcast.putExtra(EXTRA_DATA, data.getValue());
						LocalBroadcastManager.getInstance(getContext()).sendBroadcast(broadcast);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				})
				.enqueue();
	}
}
