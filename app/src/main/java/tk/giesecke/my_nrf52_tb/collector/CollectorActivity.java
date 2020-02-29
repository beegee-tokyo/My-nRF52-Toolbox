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
package tk.giesecke.my_nrf52_tb.collector;

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
import android.os.Environment;
import android.os.Handler;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.daimajia.numberprogressbar.NumberProgressBar;

import tk.giesecke.my_nrf52_tb.R;
import tk.giesecke.my_nrf52_tb.profile.BleProfileService;
import tk.giesecke.my_nrf52_tb.profile.BleProfileServiceReadyActivity;

import static android.view.View.GONE;
import static tk.giesecke.my_nrf52_tb.collector.XorCoding.xorCode;
import static tk.giesecke.my_nrf52_tb.profile.BleProfileActivity.doReconnect;

/**
 * Modify the Template Activity to match your needs.
 */
public class CollectorActivity extends BleProfileServiceReadyActivity<CollectorService.TemplateBinder> {
	@SuppressWarnings("unused")
	private final String TAG = "CollectorActivity";

	static String ssidPrimString = "";
	static String pwPrimString = "";
	static String ssidSecString = "";
	static String pwSecString = "";
	static String devIp = "";
	static String devAp = "";
	static Boolean canUpdate = false;
	static byte regionSelected = 0;
	UpdateServer updateServer;
	static int updateProgress = 0;

	static String mmDevice;

//	EditText ssidPrimET;
	EditText pwPrimET;
//	EditText ssidSecET;
	EditText pwSecET;
	Spinner ssidPrimSp;
	Spinner ssidSecSp;
	Spinner regionSelect;
	NumberProgressBar upd_fw_pb;
	private TextView upd_status_tv;

	List<String> ssidPrimList;
	List<String> ssidSecList;
	WifiManager mainWifi;
	ArrayAdapter<String> ssidPrimAdapter;
	ArrayAdapter<String> ssidSecAdapter;

	static Context appContext;

	private Menu thisMenu;

	/** Available firmware files for update */
	File[] files;

	/** Selected file for update */
	public static String updateFilename;

	@Override
	protected void onCreateView(final Bundle savedInstanceState) {
		setContentView(R.layout.activity_feature_collector);

		appContext = this;

		setGUI();
	}

	@SuppressLint("SourceLockedOrientationActivity")
	private void setGUI() {
		int currentOrientation = getResources().getConfiguration().orientation;
		if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		}

//		ssidPrimET = findViewById(R.id.ssidPrim);
		pwPrimET = findViewById(R.id.pwPrim);
//		ssidSecET = findViewById(R.id.ssidSec);
		pwSecET = findViewById(R.id.pwSec);
		upd_fw_pb = findViewById(R.id.pb_upd_fw);
		upd_status_tv = findViewById(R.id.tv_upd_stat);
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
//					String item = parent.getItemAtPosition(position).toString();
//					ssidPrimET.setText(item);
					Log.d(TAG, "User selected primary SSID " + ssidPrimString);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				ssidPrimString = "";
//				ssidPrimET.setText("");
			}
		});
		ssidSecSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				((TextView)parent.getChildAt(0)).setTextColor(Color.rgb(0x00, 0x00, 0x00));
				if (position > 0) {
					ssidSecString = parent.getItemAtPosition(position).toString();
//					String item = parent.getItemAtPosition(position).toString();
//					ssidSecET.setText(item);
					Log.d(TAG, "User selected secondary SSID " + ssidSecString);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				ssidSecString = "";
//				ssidSecET.setText("");
			}
		});

		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		registerReceiver(mScanResultReceiver, intentFilter);

//		registerReceiver(new BroadcastReceiver()
//		{
//			@Override
//			public void onReceive(Context c, Intent intent)
//			{
//				List<ScanResult> scanResults = mainWifi.getScanResults();
//				int resultSize = scanResults.size();
//				for (int idx = 0; idx < resultSize; idx++)
//				{
//					ssidPrimList.add(scanResults.get(idx).SSID);
//					ssidSecList.add(scanResults.get(idx).SSID);
//				}
//				((TextView)ssidPrimSp.getChildAt(0)).setTextColor(Color.rgb(0x00, 0x00, 0x00));
////				ssidPrimAdapter.notifyDataSetChanged();
//				((TextView)ssidSecSp.getChildAt(0)).setTextColor(Color.rgb(0x00, 0x00, 0x00));
////				ssidSecAdapter.notifyDataSetChanged();
//				((TextView)regionSelect.getChildAt(0)).setTextColor(Color.rgb(0x00, 0x00, 0x00));
//			}
//		}, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

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

	private final BroadcastReceiver mScanResultReceiver = new BroadcastReceiver() {
		@SuppressLint("SimpleDateFormat")
		@Override
		public void onReceive(final Context context, final Intent intent) {
			List<ScanResult> scanResults = mainWifi.getScanResults();
			int resultSize = scanResults.size();
			for (int idx = 0; idx < resultSize; idx++)
			{
				ssidPrimList.add(scanResults.get(idx).SSID);
				ssidSecList.add(scanResults.get(idx).SSID);
			}
			((TextView)ssidPrimSp.getChildAt(0)).setTextColor(Color.rgb(0x00, 0x00, 0x00));
//				ssidPrimAdapter.notifyDataSetChanged();
			((TextView)ssidSecSp.getChildAt(0)).setTextColor(Color.rgb(0x00, 0x00, 0x00));
//				ssidSecAdapter.notifyDataSetChanged();
			((TextView)regionSelect.getChildAt(0)).setTextColor(Color.rgb(0x00, 0x00, 0x00));
		}
	};
	
	@Override
	protected void onInitialize(final Bundle savedInstanceState) {
		LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, makeIntentFilter());
	}

	@Override
	protected void onStop() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mScanResultReceiver);
		super.onStop();
	}

	@Override
	protected void setDefaultUI() {
		// TODO clear your UI
//		ssidPrimET.setText(ssidPrimString);
		pwPrimET.setText(pwPrimString);
//		ssidSecET.setText(ssidSecString);
		pwSecET.setText(pwSecString);
	}

	@Override
	protected int getAboutTextId() {
		return R.string.collector_about_text;
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.collector_menu, menu);
		thisMenu = menu;
		return true;
	}

	@Override
	protected boolean onOptionsItemSelected(final int itemId) {
		if (itemId == R.id.connect) {
			onMenuConnectClicked();
		}
		return true;
	}

	@Override
	protected int getDefaultDeviceName() {
		return R.string.scanner_default_name;
	}

	@Override
	protected UUID getFilterUUID() {
		// TODO this method may return the UUID of the service that is required to be in the advertisement packet of a device in order to be listed on the Scanner dialog.
		// If null is returned no filtering is done.
		return CollectorManager.COLLECTOR_SERVICE_UUID;
	}

	@Override
	protected Class<? extends BleProfileService> getServiceClass() {
		return CollectorService.class;
	}

	@Override
	protected void onServiceBound(final CollectorService.TemplateBinder binder) {
		// not used
	}

	@Override
	protected void onServiceUnbound() {
		// not used
	}

	@Override
	public void onServicesDiscovered(final BluetoothDevice device, final boolean optionalServicesFound) {
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
	public void onDeviceDisconnected(final BluetoothDevice device) {
		super.onDeviceDisconnected(device);
		thisMenu.findItem(R.id.connect).setIcon(null);
		thisMenu.findItem(R.id.connect).setTitle(getString(R.string.action_connect));
		Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.collector_feature_title));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		Button enaBt = findViewById(R.id.readBT);
		enaBt.setEnabled(false);
		enaBt = findViewById(R.id.writeBT);
		enaBt.setEnabled(false);
		enaBt = findViewById(R.id.eraseBT);
		enaBt.setEnabled(false);
		enaBt = findViewById(R.id.resetBT);
		enaBt.setEnabled(false);
		enaBt = findViewById(R.id.updateBT);
		enaBt.setVisibility(GONE);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

		// Disconnect from the device
		onMenuConnectClicked();
	}

	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@SuppressLint("SimpleDateFormat")
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
			if (CollectorService.BROADCAST_DATA_RECVD.equals(action)) {
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

//				ssidPrimET.setText(ssidPrimString);
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
//				ssidSecET.setText(ssidSecString);
				pwSecET.setText(pwSecString);

//				TextView chgHdr;
//				EditText chgEt;
//				chgHdr = findViewById(R.id.ssidSecHdr);
//				chgHdr.setVisibility(View.VISIBLE);
//				chgEt = findViewById(R.id.ssidSec);
//				chgEt.setVisibility(View.VISIBLE);
//				chgHdr = findViewById(R.id.pwSecHdr);
//				chgHdr.setVisibility(View.VISIBLE);
//				chgEt = findViewById(R.id.pwSec);
//				chgEt.setVisibility(View.VISIBLE);

				regionSelect.setSelection(regionSelected);

				if (canUpdate) {
					File dir = new File(Environment.getExternalStorageDirectory() + "/SRC_Portal/");

					// list the files using a anonymous FileFilter
					files = dir.listFiles(file -> file.getName().startsWith("firmware_gateway"));

					LinearLayout updating = findViewById(R.id.ll_update);
					if (updating.getVisibility() == View.GONE) {
					Button updateBt = findViewById(R.id.updateBT);
					if ((files != null) && (files.length != 0)) {
						updateBt.setVisibility(View.VISIBLE);
					} else {
						canUpdate = false;
							updateBt.setVisibility(GONE);
						}
					}
				}
			}
		}
	};

	private static IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(CollectorService.BROADCAST_DATA_RECVD);
		intentFilter.addAction(CollectorService.BROADCAST_BATTERY_LEVEL);
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

		return intentFilter;
	}

	public void onClickWrite(View v) {
		// Update credentials with last edit text values
//		ssidPrimString = ssidPrimET.getText().toString();
		pwPrimString = pwPrimET.getText().toString();
//		ssidSecString = ssidSecET.getText().toString();
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

	@SuppressWarnings("unused")
	public void onClickRead(View v) {
		getService().readSettings();
	}

	public void onClickUpdate(View v) {
		if (canUpdate) {
			FileChooser fileChooser = new FileChooser(CollectorActivity.this);

			fileChooser.setExtension();

			Button updBt = findViewById(R.id.updateBT);
			updBt.setVisibility(GONE);

			fileChooser.setFileListener(file -> {
				updateFilename = file.getName();
				Log.i("File Name", updateFilename);

				// Create JSON object
				JSONObject wifiCreds = new JSONObject();
				try {
					wifiCreds.put("update", "update");
					wifiCreds.put("ip", UpdateServer.getIPAddress());
					wifiCreds.put("port", 12345);
					wifiCreds.put("file", updateFilename);
				} catch (JSONException e) {
					e.printStackTrace();
					return;
				}
				byte[] decodedData = xorCode(mmDevice
						, wifiCreds.toString().getBytes()
						, wifiCreds.toString().length());
				getService().writeSettings(new String(decodedData));
				// Rest has to be done on UI thread
				runOnUiThread(() -> {
					// Connected to the device AP, start OTA server now
					// Start the OTA server
					try {
						if (updateServer == null) {
							updateServer = new UpdateServer();
						}
						if (!updateServer.isAlive()) {
							updateServer.start();
						}
						LinearLayout viewProgress = findViewById(R.id.ll_update);
						viewProgress.setVisibility(View.VISIBLE);
						upd_fw_pb.setProgress(0);
						upd_status_tv.setText(getText(R.string.upd_req_ota));
						// Start timer to check update status
						Handler handler = new Handler();
						int delay = 1000; //milliseconds

						handler.postDelayed(new Runnable(){
							boolean closeWindow = false;
							public void run(){
								if (updateProgress <= 100)
								{
									if (updateProgress > 1) {
										upd_status_tv.setText(getText(R.string.upd_fw_update));
									}
									upd_fw_pb.setProgress(updateProgress);
									getService().readSettings();
									handler.postDelayed(this, delay);
								} else {
									if (!closeWindow) {
										if (updateProgress == 110) {
											upd_status_tv.setText(getText(R.string.upd_fw_succ));
											upd_fw_pb.setProgress(100);
										} else {
											upd_status_tv.setText(getText(R.string.upd_fw_fail));
											upd_fw_pb.setProgress(0);
										}
//										try {
//											wifiCreds.put("reset", "reset");
//										} catch (JSONException e) {
//											e.printStackTrace();
//										}
//										byte[] decodedData = xorCode(mmDevice
//												, wifiCreds.toString().getBytes()
//												, wifiCreds.toString().length());
//										getService().writeSettings(new String(decodedData));
										closeWindow = true;
										updateServer.stop();
										doReconnect = false;
										mService.disconnect();
//										onMenuConnectClicked();
										handler.postDelayed(this, 2000);
									} else {
										LinearLayout viewProgress = findViewById(R.id.ll_update);
										viewProgress.setVisibility(GONE);
										doReconnect = false;
									}
								}
							}
						}, delay);
					} catch (IOException ignore) {
						Toast.makeText(getApplicationContext()
								, "Cannot start Update server"
								, Toast.LENGTH_LONG).show();
					}
				});
			});
// Set up and filter my extension I am looking for
			//fileChooser.setExtension("pdf");
			fileChooser.showDialog();


		}
	}

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
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
		doReconnect = false;
		mService.disconnect();
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
//		if (pwVisible) {
//			EditText pwEditText = findViewById(R.id.pwPrim);
//			pwEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
//			pwEditText = findViewById(R.id.pwSec);
//			pwEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
//		}
//		else
//		{
//			EditText pwEditText = findViewById(R.id.pwPrim);
//			pwEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
//			pwEditText = findViewById(R.id.pwSec);
//			pwEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
//		}
	}
}
