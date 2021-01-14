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
package tk.giesecke.my_nrf52_tb.esp32_setup;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import no.nordicsemi.android.ble.data.Data;
import tk.giesecke.my_nrf52_tb.esp32_setup.callback.ESP32_Settings_ManagerCallbacks;
import tk.giesecke.my_nrf52_tb.profile.BleManager;

import static tk.giesecke.my_nrf52_tb.esp32_setup.ESP32SettingsService.BROADCAST_DATA_RECVD;
import static tk.giesecke.my_nrf52_tb.esp32_setup.ESP32SettingsService.EXTRA_DATA;
import static tk.giesecke.my_nrf52_tb.esp32_setup.XorCoding.xorCode;
import static tk.giesecke.my_nrf52_tb.profile.BleProfileService.EXTRA_DEVICE;

/**
 * Modify to Collector manager to match your requirements.
 * The CollectorManager extends  {@link BleManager}
 */
public class ESP32_Settings_Manager extends BleManager<ESP32_Settings_ManagerCallbacks> {
	/**
	 * The Collector service UUID.
	 */
	static final UUID COLLECTOR_SERVICE_UUID = UUID.fromString("0000aaaa-ead2-11e7-80c1-9a214cf093ae"); // Collector service ID
	/**
	 * The WiFi settings UUID. 0000aaaa-ead2-11e7-80c1-9a214cf093ae
	 */
	private static final UUID WIFI_SETTINGS_UUID = UUID.fromString("00005555-ead2-11e7-80c1-9a214cf093ae"); // WiFi settings

	private BluetoothGattCharacteristic mRequiredCharacteristic, mSettingsCharacteristic;

	ESP32_Settings_Manager(final Context context) {
		super(context);
	}

	@NonNull
	@Override
	protected BleManagerGattCallback  getGattCallback() {
		return mGattCallback;
	}

	/**
	 * BluetoothGatt callbacks for connection/disconnection, service discovery,
	 * receiving indication, etc.
	 */
	private final BleManagerGattCallback  mGattCallback = new BleManagerGattCallback () {

		@Override
		protected void initialize() {
			// Initialize the Battery Manager. It will enable Battery Level notifications.
			// Remove it if you don't need this feature.
			super.initialize();

			// Increase the MTU
			requestMtu(128)
					.with((device, mtu) -> {
						//log(LogContract.Log.Level.APPLICATION, "MTU changed to " + mtu);
					})
					.done(device -> {
						// You may do some logic in here that should be done when the request finished successfully.
						// In case of MTU this method is called also when the MTU hasn't changed, or has changed
						// to a different (lower) value. Use .with(...) to get the MTU value.
						Log.d("COLL_MAN", "MTU size changed");
					})
					.fail((device, status) -> Log.d("COLL_MAN", "MTU size change failed"))
					.enqueue();
		}

		@Override
		protected boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
			// It should return true if all has been discovered (that is that device is supported).
			final BluetoothGattService service = gatt.getService(COLLECTOR_SERVICE_UUID);
			if (service != null) {
				mRequiredCharacteristic = service.getCharacteristic(WIFI_SETTINGS_UUID);
				mSettingsCharacteristic = service.getCharacteristic(WIFI_SETTINGS_UUID);
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

			readSettings();
//			readCharacteristic(mSettingsCharacteristic)
//					.with((device, data) -> {
//						// Characteristic value has been read
//						// Let's do some magic with it.
//						CollectorActivity.mmDevice = device.getName();
//						// Decode the data
//						byte[] decodedData = xorCode(device.getName(),data.getValue(),data.size());
//						String finalData = new String(decodedData);
//
//						// Get stored WiFi credentials from the received data
//						JSONObject receivedConfigJSON;
//						try {
//							receivedConfigJSON = new JSONObject(finalData);
//							if (receivedConfigJSON.has("ssidPrim")) {
//								CollectorActivity.ssidPrimString = receivedConfigJSON.getString("ssidPrim");
//							}
//							if (receivedConfigJSON.has("pwPrim")) {
//								CollectorActivity.pwPrimString = receivedConfigJSON.getString("pwPrim");
//							}
//							if (receivedConfigJSON.has("ssidSec")) {
//								CollectorActivity.ssidSecString = receivedConfigJSON.getString("ssidSec");
//							}
//							if (receivedConfigJSON.has("pwSec")) {
//								CollectorActivity.pwSecString = receivedConfigJSON.getString("pwSec");
//							}
//							if (receivedConfigJSON.has("ip") && receivedConfigJSON.has("ap")) {
//								CollectorActivity.devIp = receivedConfigJSON.getString("ip");
//								CollectorActivity.devAp = receivedConfigJSON.getString("ap");
//								WifiManager mWiFiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//								WifiInfo w = mWiFiManager.getConnectionInfo();
//								String mySSID = w.getSSID();
//								mySSID = mySSID.substring(1,mySSID.length()-1);
//								if (CollectorActivity.devAp.equalsIgnoreCase(mySSID)) {
//									CollectorActivity.canUpdate = true;
//								} else {
//									CollectorActivity.canUpdate = false;
//								}
//							} else {
//								CollectorActivity.canUpdate = false;
//							}
//							if (receivedConfigJSON.has("progress")) {
//								CollectorActivity.updateProgress = receivedConfigJSON.getInt("progress");
//							}
//							final Intent broadcast = new Intent(BROADCAST_DATA_RECVD);
//							broadcast.putExtra(EXTRA_DEVICE, getBluetoothDevice());
//							broadcast.putExtra(EXTRA_DATA, data.getValue());
//							LocalBroadcastManager.getInstance(getContext()).sendBroadcast(broadcast);
//						} catch (JSONException e) {
//							e.printStackTrace();
//						}
//					})
//					.enqueue();
		}
	};

	// TODO Define manager's API

	/**
	 * This method will write important data to the device.
	 *
	 * @param parameter parameter to be written.
	 */
	void writeSettings(final String parameter) {
		// Write some data to the characteristic.
		writeCharacteristic(mSettingsCharacteristic, Data.from(parameter))
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
		readCharacteristic(mSettingsCharacteristic)
				.with((device, data) -> {
					// Characteristic value has been read
					// Let's do some magic with it.
					ESP32_Settings_Activity.mmDevice = device.getName();
					// Decode the data
					byte[] decodedData = xorCode(device.getName(),data.getValue(),data.size());
					String finalData = new String(decodedData);

					// Get stored WiFi credentials from the received data
					JSONObject receivedConfigJSON;
					try {
						receivedConfigJSON = new JSONObject(finalData);
						if (receivedConfigJSON.has("ssidPrim")) {
							ESP32_Settings_Activity.ssidPrimString = receivedConfigJSON.getString("ssidPrim");
						}
						if (receivedConfigJSON.has("pwPrim")) {
							ESP32_Settings_Activity.pwPrimString = receivedConfigJSON.getString("pwPrim");
						}
						if (receivedConfigJSON.has("ssidSec")) {
							ESP32_Settings_Activity.ssidSecString = receivedConfigJSON.getString("ssidSec");
						}
						if (receivedConfigJSON.has("pwSec")) {
							ESP32_Settings_Activity.pwSecString = receivedConfigJSON.getString("pwSec");
						}
						if (receivedConfigJSON.has("lora")) {
							ESP32_Settings_Activity.regionSelected = (byte) receivedConfigJSON.getInt("lora");
						}
						if (receivedConfigJSON.has("ip") && receivedConfigJSON.has("ap")) {
							ESP32_Settings_Activity.devIp = receivedConfigJSON.getString("ip");
							ESP32_Settings_Activity.devAp = receivedConfigJSON.getString("ap");
							WifiManager mWiFiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
							WifiInfo w = mWiFiManager.getConnectionInfo();
							String mySSID = w.getSSID();
							mySSID = mySSID.substring(1,mySSID.length()-1);
							ESP32_Settings_Activity.canUpdate = ESP32_Settings_Activity.devAp.equalsIgnoreCase(mySSID);
						} else {
							ESP32_Settings_Activity.canUpdate = false;
						}
						if (receivedConfigJSON.has("progress")) {
							ESP32_Settings_Activity.updateProgress = receivedConfigJSON.getInt("progress");
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
