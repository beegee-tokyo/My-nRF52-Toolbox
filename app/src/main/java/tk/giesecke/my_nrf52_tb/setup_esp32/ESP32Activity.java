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

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import tk.giesecke.my_nrf52_tb.AppHelpFragment;
import tk.giesecke.my_nrf52_tb.R;
import tk.giesecke.my_nrf52_tb.profile.BleProfileService;
import tk.giesecke.my_nrf52_tb.profile.BleProfileServiceReadyActivity;
import tk.giesecke.my_nrf52_tb.setup_esp32.settings.SettingsActivity;

import static tk.giesecke.my_nrf52_tb.setup_esp32.XorCoding.xorCode;

/**
 * Modify the Template Activity to match your needs.
 */
public class ESP32Activity extends BleProfileServiceReadyActivity<ESP32Service.ESP32Binder> {

	private final String TAG = "ESP32Activity";

	static String ssidPrimString = "";
	static String pwPrimString = "";
	static String ssidSecString = "";
	static String pwSecString = "";
	static byte regionSelected = 0;

//	static String fcm0String = "";
//	static String fcm1String = "";
//	static String fcm2String = "";
//	static String fcm3String = "";
//	static String fcm4String = "";
//	static String fcm5String = "";
//	static String fcm6String = "";
//	static String fcm7String = "";
//	static String fcm8String = "";
//	static String fcm9String = "";

	static String mmDevice;

	EditText ssidPrimET;
	EditText pwPrimET;
	EditText ssidSecET;
	EditText pwSecET;
	Spinner ssidPrimSp;
	Spinner ssidSecSp;
	Spinner regionSelect;

	List<String> ssidPrimList;
	List<String> ssidSecList;
	WifiManager mainWifi;
	ArrayAdapter<String> ssidPrimAdapter;
	ArrayAdapter<String> ssidSecAdapter;

	private Menu thisMenu;

	Boolean apManualActive = false;

	@Override
	protected void onCreateView(final Bundle savedInstanceState) {
		// TODO modify the layout file(s). By default the activity shows only one field - the Heart Rate value as a sample
		setContentView(R.layout.activity_feature_esp32);
		setGUI();
	}

	private void setGUI() {
		int currentOrientation = getResources().getConfiguration().orientation;
		if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		}

		ssidPrimET = findViewById(R.id.ssidPrim);
		ssidPrimET.setVisibility(View.INVISIBLE);
		pwPrimET = findViewById(R.id.pwPrim);
		ssidSecET = findViewById(R.id.ssidSec);
		ssidSecET.setVisibility(View.INVISIBLE);
		pwSecET = findViewById(R.id.pwSec);
		ssidPrimSp = findViewById(R.id.sp_ssid_prim);
		ssidSecSp = findViewById(R.id.sp_ssid_sec);

		ssidPrimList = new ArrayList<>();
		ssidSecList = new ArrayList<>();

		ssidPrimList.add(getResources().getString(R.string.wifi_sel_hint));
		ssidSecList.add(getResources().getString(R.string.wifi_sel_hint));

		ssidPrimAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ssidPrimList);
		ssidSecAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ssidSecList);

		// Drop down layout style - list view with radio button
		ssidPrimAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		ssidSecAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// attaching data adapter to spinner
		ssidPrimSp.setAdapter(ssidPrimAdapter);
		ssidSecSp.setAdapter(ssidSecAdapter);

		ssidPrimSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				((TextView)parent.getChildAt(0)).setTextColor(Color.rgb(0x00, 0x00, 0x00));
				if (position > 0) {
					ssidPrimString = parent.getItemAtPosition(position).toString();
					ssidPrimET.setText(ssidPrimString);
					Log.d(TAG, "User selected primary SSID " + ssidPrimString);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				ssidPrimString = "";
			}
		});
		ssidSecSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				((TextView)parent.getChildAt(0)).setTextColor(Color.rgb(0x00, 0x00, 0x00));
				if (position > 0) {
					ssidSecString = parent.getItemAtPosition(position).toString();
					ssidSecET.setText(ssidSecString);
					Log.d(TAG, "User selected secondary SSID " + ssidSecString);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				ssidSecString = "";
			}
		});

		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		registerReceiver(wifiScanResultReceiver, intentFilter);

		mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if (!mainWifi.startScan()) {
			Log.e(TAG, "WiFi scan not started!");
		}

		regionSelect = findViewById(R.id.sp_region);
		ArrayAdapter<CharSequence> regionAdp = ArrayAdapter
				.createFromResource(this, R.array.region_list,
						android.R.layout.simple_spinner_item);
		regionAdp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		regionSelect.setAdapter(regionAdp);


		regionSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int position, long id) {
				regionSelected = (byte)position;
				Log.d(TAG, "Selected region: " + regionSelected);
				((TextView)regionSelect.getChildAt(0)).setTextColor(Color.rgb(0x00, 0x00, 0x00));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	@Override
	protected void onInitialize(final Bundle savedInstanceState) {
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, makeIntentFilter());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
	}

	@Override
	protected void setDefaultUI() {
		ssidPrimET.setText(ssidPrimString);
		pwPrimET.setText(pwPrimString);
		ssidSecET.setText(ssidSecString);
		pwSecET.setText(pwSecString);
		if ((service != null) && !service.isConnected()){
			thisMenu.findItem(R.id.connect).setIcon(R.drawable.ic_action_bluetooth);
		}
	}

	@Override
	protected int getLoggerProfileTitle() {
		return R.string.esp32_feature_title;
	}

	@Override
	protected int getAboutTextId() {
		return R.string.esp32_about_text;
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.connect_and_about_menu, menu);
		thisMenu = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
		} else if (item.getItemId() == R.id.action_about) {
			final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.esp32_about_text);
			fragment.show(getSupportFragmentManager(), "help_fragment");
		} else if (item.getItemId() == R.id.action_settings) {
			final Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
		} else if (item.getItemId() == R.id.connect) {
			if ((service != null) && service.isConnected()){
				thisMenu.findItem(R.id.connect).setIcon(R.drawable.ic_action_bluetooth);
			}
			onMenuConnectClicked();
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

		// Disconnect from the device
		onMenuConnectClicked();
	}

	@Override
	protected int getDefaultDeviceName() {
		return R.string.template_default_name;
	}

	@Override
	protected UUID getFilterUUID() {
		// TODO this method may return the UUID of the service that is required to be in the advertisement packet of a device in order to be listed on the Scanner dialog.
		// If null is returned no filtering is done.
		return ESP32Manager.ESP32_SERVICE_UUID;
	}

	@Override
	protected Class<? extends BleProfileService> getServiceClass() {
		return ESP32Service.class;
	}

	@Override
	protected void onServiceBound(final ESP32Service.ESP32Binder binder) {
		// not used
	}

	@Override
	protected void onServiceUnbound() {
		// not used
	}

	@Override
	public void onServicesDiscovered(@NonNull final BluetoothDevice device, final boolean optionalServicesFound) {
		thisMenu.findItem(R.id.connect).setTitle("");
		thisMenu.findItem(R.id.connect).setIcon(android.R.drawable.ic_delete);
		Button enaBt = findViewById(R.id.readBT);
		enaBt.setEnabled(true);
		enaBt = findViewById(R.id.writeBT);
		enaBt.setEnabled(true);
		enaBt = findViewById(R.id.eraseBT);
		enaBt.setEnabled(true);
		enaBt = findViewById(R.id.resetBT);
		enaBt.setEnabled(true);
	}

	@Override
	public void onDeviceDisconnected(@NonNull final BluetoothDevice device) {
		super.onDeviceDisconnected(device);
		thisMenu.findItem(R.id.connect).setIcon(R.drawable.ic_action_bluetooth);
		thisMenu.findItem(R.id.connect).setTitle(getString(R.string.action_connect));
		Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.esp32_feature_title));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		Button enaBt = findViewById(R.id.readBT);
		enaBt.setEnabled(false);
		enaBt = findViewById(R.id.writeBT);
		enaBt.setEnabled(false);
		enaBt = findViewById(R.id.eraseBT);
		enaBt.setEnabled(false);
		enaBt = findViewById(R.id.resetBT);
		enaBt.setEnabled(false);
	}

	// Receive results from WiFi AP scan
	private final BroadcastReceiver wifiScanResultReceiver = new BroadcastReceiver() {
		@SuppressLint("SimpleDateFormat")
		@Override
		public void onReceive(final Context context, final Intent intent) {
			List<ScanResult> scanResults = mainWifi.getScanResults();
			int resultSize = scanResults.size();
			if (!ssidPrimList.isEmpty()) {
				ssidPrimList.clear();
				ssidSecList.clear();
			}
			for (int idx = 0; idx < resultSize; idx++)
			{
				ssidPrimList.add(scanResults.get(idx).SSID);
				ssidSecList.add(scanResults.get(idx).SSID);
			}
			((TextView)ssidPrimSp.getChildAt(0)).setTextColor(Color.rgb(0x00, 0x00, 0x00));
			((TextView)ssidSecSp.getChildAt(0)).setTextColor(Color.rgb(0x00, 0x00, 0x00));
			((TextView)regionSelect.getChildAt(0)).setTextColor(Color.rgb(0x00, 0x00, 0x00));
		}
	};


	// Handling updates from the device
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@SuppressLint("SimpleDateFormat")
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
			if (ESP32Service.BROADCAST_DATA_RECVD.equals(action)) {
				// ToDo set spinner if possible
				boolean foundSSID = false;
				for (int idx = 0; idx < ssidPrimList.size(); idx++)
				{
					if (ssidPrimList.get(idx).equalsIgnoreCase(ssidPrimString)) {
						ssidPrimSp.setSelection(idx);
						foundSSID = true;
						break;
					}
				}
				if (!foundSSID)
				{
					ssidPrimSp.setSelection(0);
				}

				ssidPrimET.setText(ssidPrimString);
				pwPrimET.setText(pwPrimString);
				// ToDo set spinner if possible
				foundSSID = false;
				for (int idx = 0; idx < ssidSecList.size(); idx++)
				{
					if (ssidSecList.get(idx).equalsIgnoreCase(ssidSecString)) {
						ssidSecSp.setSelection(idx);
						foundSSID = true;
						break;
					}
				}
				if (!foundSSID)
				{
					ssidSecSp.setSelection(0);
				}
				ssidSecET.setText(ssidSecString);
				pwSecET.setText(pwSecString);

				regionSelect.setSelection(regionSelected);
			}
		}
	};

	private static IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ESP32Service.BROADCAST_DATA_RECVD);
		intentFilter.addAction(ESP32Service.BROADCAST_BATTERY_LEVEL);
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

		return intentFilter;
	}

	public void onClickWrite(View v) {
		ssidPrimString = ssidPrimET.getText().toString();
		ssidSecString = ssidSecET.getText().toString();

		// Update credentials with last edit text values
		pwPrimString = pwPrimET.getText().toString();
		pwSecString = pwSecET.getText().toString();

		// Create JSON object
		JSONObject wifiCreds = new JSONObject();
		try {
			if (ssidPrimString.equals("")) {
				Toast.makeText(getApplicationContext()
						, "Missing primary SSID entry"
						, Toast.LENGTH_LONG).show();
				return;
			} else {
				wifiCreds.put("ssidPrim", ssidPrimString);
			}
			if (pwPrimString.equals("")) {
				Toast.makeText(getApplicationContext()
						, "Missing primary password entry"
						, Toast.LENGTH_LONG).show();
				return;
			} else {
				wifiCreds.put("pwPrim", pwPrimString);
			}
			if (ssidSecString.equals("")) {
				wifiCreds.put("ssidSec", ssidPrimString);
			} else {
				wifiCreds.put("ssidSec", ssidSecString);
			}
			if (pwSecString.equals("")) {
				wifiCreds.put("pwSec", pwPrimString);
			} else {
				wifiCreds.put("pwSec", pwSecString);
			}
			wifiCreds.put("lora", regionSelected);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		byte[] decodedData = xorCode(mmDevice
				, wifiCreds.toString().getBytes()
				, wifiCreds.toString().length());
		getService().writeSettings(new String(decodedData));
	}

	public void onClickRead(View v) {
		getService().readSettings();
	}

	public void onClickErase(View v){
		// Create JSON object
		JSONObject wifiCreds = new JSONObject();
		try {
			wifiCreds.put("erase", true);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		byte[] decodedData = xorCode(mmDevice
				,wifiCreds.toString().getBytes()
				,wifiCreds.toString().length());
		getService().writeSettings(new String(decodedData));
	}

	public void onClickReset(View v){
		// Create JSON object
		JSONObject wifiCreds = new JSONObject();
		try {
			wifiCreds.put("reset", true);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		byte[] decodedData = xorCode(mmDevice
				,wifiCreds.toString().getBytes()
				,wifiCreds.toString().length());
		getService().writeSettings(new String(decodedData));
		service.disconnect();
	}

	public void onClickShowPassword(View v) {
		Button pwButton = findViewById(R.id.bt_showPW);
		if (pwButton.getText().toString().equals(getResources().getString(R.string.showPW))) {
			pwButton.setText(getResources().getString(R.string.hidePW));
			EditText pwEditText = findViewById(R.id.pwPrim);
			pwEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
			pwEditText = findViewById(R.id.pwSec);
			pwEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
		} else {
			pwButton.setText(getResources().getString(R.string.showPW));
			EditText pwEditText = findViewById(R.id.pwPrim);
			pwEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
			pwEditText = findViewById(R.id.pwSec);
			pwEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
		}
	}

	public void onClickManualAP(View v) {
		Button pwButton = findViewById(R.id.bt_manAP);
		if (pwButton.getText().toString().equals(getResources().getString(R.string.manAP))) {
			pwButton.setText(getResources().getString(R.string.autoAP));
			ssidPrimSp.setVisibility(View.INVISIBLE);
			ssidPrimET.setVisibility(View.VISIBLE);
			ssidSecSp.setVisibility(View.INVISIBLE);
			ssidSecET.setVisibility(View.VISIBLE);
		} else {
			pwButton.setText(getResources().getString(R.string.manAP));
			ssidPrimSp.setVisibility(View.VISIBLE);
			ssidPrimET.setVisibility(View.INVISIBLE);
			ssidSecSp.setVisibility(View.VISIBLE);
			ssidSecET.setVisibility(View.INVISIBLE);
		}
		apManualActive = false;
	}

	public void onClickShowFCM(View v) {
		// todo Show dialog with known FCM tokens
		// todo add option to add new FCM token
		// todo add option to delete FCM token

	}
}
