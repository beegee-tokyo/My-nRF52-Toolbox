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

package tk.giesecke.my_nrf52_tb.uart;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.UUID;

import tk.giesecke.my_nrf52_tb.AppHelpFragment;
import tk.giesecke.my_nrf52_tb.R;
import tk.giesecke.my_nrf52_tb.profile.BleProfileService;
import tk.giesecke.my_nrf52_tb.profile.BleProfileServiceReadyActivity;
import tk.giesecke.my_nrf52_tb.scanner.ScannerFragment;
import tk.giesecke.my_nrf52_tb.uart.settings.SettingsActivity;

import static tk.giesecke.my_nrf52_tb.uart.UARTService.BROADCAST_UART_RX;
import static tk.giesecke.my_nrf52_tb.uart.UARTService.EXTRA_DATA;
import static tk.giesecke.my_nrf52_tb.uart.UARTService.SETTINGS_CLOSE;

public class UARTActivity extends BleProfileServiceReadyActivity<UARTService.UARTBinder> implements UARTInterface {
	private final static String TAG = "UARTActivity";

	private UARTService.UARTBinder mServiceBinder;

	private boolean userScroll = false;

	public static String[] buttonNames;
	public static String[] buttonValues;

	private Menu thisMenu;

	@Override
	protected Class<? extends BleProfileService> getServiceClass() {
		return UARTService.class;
	}

	@Override
	protected void setDefaultUI() {
		// empty
	}

	@Override
	protected void onServiceBound(final UARTService.UARTBinder binder) {
		mServiceBinder = binder;
	}

	@Override
	protected void onServiceUnbound() {
		mServiceBinder = null;
	}

	@SuppressLint("DefaultLocale")
	@Override
	protected void onInitialize(final Bundle savedInstanceState) {
		LocalBroadcastManager.getInstance(this).registerReceiver(mUartReceiver, makeIntentFilter());
		onMenuConnectClicked();
	}

	@Override
	protected void onDestroy() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mUartReceiver);
		super.onDestroy();
	}

	@Override
	protected void onCreateView(final Bundle savedInstanceState) {
		setContentView(R.layout.activity_feature_uart);
	}

	Handler handler;
	Runnable thisRun;

	@Override
	protected void onViewCreated(final Bundle savedInstanceState) {
//		ActionBar mSupportActionBar = getSupportActionBar();
//		if (mSupportActionBar != null) {
//			getSupportActionBar().setDisplayShowTitleEnabled(false);
//		}
		final Toolbar toolbar  = findViewById(R.id.toolbar_actionbar);
		setSupportActionBar(toolbar);
//		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		handler = new Handler();
		thisRun = () -> {
			// Enable scrolling 5 seconds after user swiped
			userScroll = false;
		};
		EditText sendText = findViewById(R.id.send_text);
		View sendBtn = findViewById(R.id.send_btn);
		sendBtn.setOnClickListener(v -> {
			send(sendText.getText().toString());
			sendText.getText().clear();
		});
		View clearButton = findViewById(R.id.clr_btn);
		clearButton.setOnClickListener(v -> {
			TextView logOut = findViewById(R.id.rcvd_lines);
			logOut.setText("");
		});

		refreshUI();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.connect_and_about_menu, menu);
		thisMenu = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == R.id.home) {
			onBackPressed();
		} else if (item.getItemId() == R.id.action_about) {
			final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.uart_about_text);
			fragment.show(getSupportFragmentManager(), "help_fragment");
		} else if (item.getItemId() == R.id.action_settings) {
			final Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
		} else if (item.getItemId() == R.id.connect) {
			if ((service != null) && (service.isConnected())) {
				thisMenu.findItem(R.id.connect).setIcon(R.drawable.ic_action_bluetooth);
			}
			onMenuConnectClicked();
		} else {
			onBackPressed();
		}
		return true;
	}

	@SuppressLint("DefaultLocale")
	void refreshUI() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		Button changeBt;

		buttonNames = new String[10];
		buttonValues = new String[10];

		for (int idx = 0; idx < 10; idx++) {
			buttonNames[idx] = buttonValues[idx] = "";
		}
		for (int idx = 0; idx < 10; idx++) {
			switch (idx) {
				case 0:
					changeBt = findViewById(R.id.bt0);
					changeBt.setOnClickListener(v -> {
						EditText sentText = findViewById(R.id.send_text);
						sentText.setText(buttonValues[0]);
					});
					break;
				case 1:
					changeBt = findViewById(R.id.bt1);
					changeBt.setOnClickListener(v -> {
						EditText sentText = findViewById(R.id.send_text);
						sentText.setText(buttonValues[1]);
					});
					break;
				case 2:
					changeBt = findViewById(R.id.bt2);
					changeBt.setOnClickListener(v -> {
						EditText sentText = findViewById(R.id.send_text);
						sentText.setText(buttonValues[2]);
					});
					break;
				case 3:
					changeBt = findViewById(R.id.bt3);
					changeBt.setOnClickListener(v -> {
						EditText sentText = findViewById(R.id.send_text);
						sentText.setText(buttonValues[3]);
					});
					break;
				case 4:
					changeBt = findViewById(R.id.bt4);
					changeBt.setOnClickListener(v -> {
						EditText sentText = findViewById(R.id.send_text);
						sentText.setText(buttonValues[4]);
					});
					break;
				case 5:
					changeBt = findViewById(R.id.bt5);
					changeBt.setOnClickListener(v -> {
						EditText sentText = findViewById(R.id.send_text);
						sentText.setText(buttonValues[5]);
					});
					break;
				case 6:
					changeBt = findViewById(R.id.bt6);
					changeBt.setOnClickListener(v -> {
						EditText sentText = findViewById(R.id.send_text);
						sentText.setText(buttonValues[6]);
					});
					break;
				case 7:
					changeBt = findViewById(R.id.bt7);
					changeBt.setOnClickListener(v -> {
						EditText sentText = findViewById(R.id.send_text);
						sentText.setText(buttonValues[7]);
					});
					break;
				case 8:
					changeBt = findViewById(R.id.bt8);
					changeBt.setOnClickListener(v -> {
						EditText sentText = findViewById(R.id.send_text);
						sentText.setText(buttonValues[8]);
					});
					break;
				default:
					changeBt = findViewById(R.id.bt9);
					changeBt.setOnClickListener(v -> {
						EditText sentText = findViewById(R.id.send_text);
						sentText.setText(buttonValues[9]);
					});
					break;
			}
			buttonNames[idx] = sharedPreferences.getString((String.format("bt%d_name_value", idx)), String.format("B%d", idx));
			changeBt.setText(buttonNames[idx]);
			buttonValues[idx] = sharedPreferences.getString((String.format("bt%d_value", idx)), "");
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_UP:
				userScroll = true;
				handler.removeCallbacks(thisRun);
				handler.postDelayed(thisRun, 5000);
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_MOVE:
				break;
		}

		return super.dispatchTouchEvent(ev);
	}

	@Override
	protected void onRestoreInstanceState(final @NonNull Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onServicesDiscovered(final BluetoothDevice device, final boolean optionalServicesFound) {
		thisMenu.findItem(R.id.connect).setTitle("");
		thisMenu.findItem(R.id.connect).setIcon(android.R.drawable.ic_delete);
	}

	@Override
	public void onDeviceSelected(final BluetoothDevice device, final String name) {
		// The super method starts the service
		super.onDeviceSelected(device, name);
	}

	@Override
	protected int getDefaultDeviceName() {
		return R.string.scanner_default_name;
	}

	@Override
	protected int getAboutTextId() {
		return R.string.uart_about_text;
	}

	@Override
	protected UUID getFilterUUID() {
		return null; // not used
	}

	@Override
	public void send(final String text) {
		if (mServiceBinder != null)
			mServiceBinder.send(text);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		// Disconnect from the device
		onMenuConnectClicked();
	}

	private final BroadcastReceiver mUartReceiver = new BroadcastReceiver() {
		@SuppressLint("SimpleDateFormat")
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();

			if (BROADCAST_UART_RX.equals(action)) {
				TextView logOut = findViewById(R.id.rcvd_lines);
				String rcvd = intent.getStringExtra(EXTRA_DATA);
				StringBuilder rcvdRaw = new StringBuilder();
				boolean addNL = false;
				if (rcvd != null) {
					if (((rcvd.charAt(0) >= 32) && (rcvd.charAt(0) < 127))
							|| (rcvd.charAt(0) == 10)
							|| (rcvd.charAt(0) == 13)) {
						rcvd = intent.getStringExtra(EXTRA_DATA);
					} else {
						for (int idx = 0; idx < rcvd.length(); idx++) {
							rcvdRaw.append(String.format("0x%02X", (byte) rcvd.charAt(idx)));
							addNL = true;
						}
						rcvd = rcvdRaw.toString();
					}
				}
				logOut.append(rcvd);
				if (addNL) {
					logOut.append("\n");
				}
				Log.d(TAG, "Received: " + rcvd);

				// Check length of content and shorten it if there are more than 250 lines
				String[] strArr = logOut.getText().toString().split("\\n");
				if (strArr.length > 240) {
					StringBuilder newStr = new StringBuilder();
					for (int idx = 10; idx < strArr.length; idx++) {
						newStr.append(strArr[idx]);
						newStr.append("\n");
					}
					logOut.setText(newStr.toString());
				}

				if (!userScroll) {
					NestedScrollView sv = findViewById(R.id.sv_rcvdData);
					sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
				}
			}
			if (SETTINGS_CLOSE.equals(action)) {
				refreshUI();
			}
		}
	};

	private static IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BROADCAST_UART_RX);
		intentFilter.addAction(SETTINGS_CLOSE);
		return intentFilter;
	}

	/**
	 * Callback of CONNECT/DISCONNECT button on DfuActivity
	 */
	public void onConnectClicked(final View view) {
		if (isBLEEnabled()) {
			if (service == null) {
				setDefaultUI();
				showDeviceScanningDialog();
			} else {
				service.disconnect();
				thisMenu.findItem(R.id.connect).setIcon(R.drawable.ic_action_bluetooth);
			}
		} else {
			showBLEDialog();
		}
	}

	private void showDeviceScanningDialog() {
		final ScannerFragment dialog = ScannerFragment.getInstance(null); // Device that is advertising directly does not have the GENERAL_DISCOVERABLE nor LIMITED_DISCOVERABLE flag set.
		dialog.show(getSupportFragmentManager(), "scan_fragment");
	}

}
