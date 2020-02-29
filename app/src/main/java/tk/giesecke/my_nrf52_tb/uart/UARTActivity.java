package tk.giesecke.my_nrf52_tb.uart;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

import tk.giesecke.my_nrf52_tb.FeaturesActivity;
import tk.giesecke.my_nrf52_tb.R;
import tk.giesecke.my_nrf52_tb.profile.BleProfileActivity;
import tk.giesecke.my_nrf52_tb.profile.BleProfileService;
import tk.giesecke.my_nrf52_tb.profile.BleProfileServiceReadyActivity;
import tk.giesecke.my_nrf52_tb.scanner.ScannerFragment;

import static tk.giesecke.my_nrf52_tb.FeaturesActivity.reqUUID;
import static tk.giesecke.my_nrf52_tb.uart.UARTService.BROADCAST_UART_RX;
import static tk.giesecke.my_nrf52_tb.uart.UARTService.EXTRA_DATA;

public class UARTActivity extends BleProfileServiceReadyActivity<UARTService.UARTBinder>
		implements UARTInterface,
		SharedPreferences.OnSharedPreferenceChangeListener {
	private final static String TAG = "UARTActivity";

	private UARTService.UARTBinder mServiceBinder;

	private TextView mDeviceNameView;

	private boolean userScroll = false;

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
		mDeviceNameView = findViewById(R.id.device_name);
	}

	@Override
	protected void onViewCreated(final Bundle savedInstanceState) {
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		ScrollView sv = findViewById((R.id.sv_rcvdData));
		Handler handler = new Handler();
		Runnable thisRun = () -> {
			//Do something after 100ms
			userScroll = false;
		};
		sv.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
			@Override
			public void onScrollChanged() {
				userScroll = true;
				handler.removeCallbacks(thisRun);
				handler.postDelayed(thisRun, 15000);
			}
		});
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
		// do nothing
	}

	@Override
	public void onDeviceSelected(final BluetoothDevice device, final String name) {
		// The super method starts the service
		super.onDeviceSelected(device, name);

		// TODO prepare output window
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
		BleProfileActivity.doReconnect = false;
		if (mService != null) {
			if (mService.isConnected()) {
				mService.disconnect();
			}
//			onMenuConnectClicked();
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("uuid_filter") || key.equals("uuid_filter_value")) {
			if (sharedPreferences.getBoolean("uuid_filter",true)) {
				String newUuidFilter = sharedPreferences.getString(("uuid_filter_value"), "");
				if (newUuidFilter.isEmpty()) {
					return;
				}
				String tempUUID = sharedPreferences.getString(("uuid_filter_value"), "");
				boolean valid128UUID = false;
				boolean valid16UUID = false;
				if(Pattern.matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", tempUUID)) {
					valid128UUID = true;
				}
				if(Pattern.matches("[a-fA-F0-9]{4}", tempUUID)) {
					valid16UUID = true;
				}
				if (valid16UUID) {
					tempUUID = "0000" + tempUUID +  "-0000-1000-8000-00805F9B34FB";
				}
				if (valid16UUID || valid128UUID) {
					try {
						reqUUID = UUID.fromString(tempUUID);
					} catch (IllegalArgumentException ignore) {
						reqUUID = null;
						Toast.makeText(this, R.string.settings_invalidUUID, Toast.LENGTH_SHORT);
					}
				}
			} else {
				reqUUID = null;
			}
			return;
		}
		if (key.equals("name_filter") || key.equals("name_filter_value")) {
			if (sharedPreferences.getBoolean("name_filter",true)) {
				FeaturesActivity.devicePrefix = sharedPreferences.getString(("name_filter_value"), "");
			} else {
				reqUUID = null;
			}
		}
	}

	int lines = 0;

	private final BroadcastReceiver mUartReceiver = new BroadcastReceiver() {
		@SuppressLint("SimpleDateFormat")
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();

			if (BROADCAST_UART_RX.equals(action)) {
				TextView logOut = findViewById(R.id.rcvd_lines);
//				byte[] rcvdRaw;
//				rcvdRaw = intent.getByteArrayExtra(EXTRA_DATA);
				String rcvd = intent.getStringExtra(EXTRA_DATA);
				String rcvdRaw = "";
				if (Character.isLetter(rcvd.charAt(0)) || Character.isDigit(rcvd.charAt(0))) {
					rcvd = intent.getStringExtra(EXTRA_DATA);
				} else {
					for (int idx = 0; idx < rcvd.length(); idx++) {
						rcvdRaw = rcvdRaw + String.format("%02X ", (byte) rcvd.charAt(idx));
					}
					rcvd = rcvdRaw;
				}
				logOut.append(rcvd);
				logOut.append("\n");
				Log.d(TAG, "Received: " + rcvd);

				// Check length of content and shorten it if there are more than 250 lines
				String[] strArr = logOut.getText().toString().split("\\n");
				if (strArr.length > 250) {
					StringBuilder newStr = new StringBuilder();
					for (int idx = 10; idx < strArr.length; idx++) {
						newStr.append(strArr[idx]);
						newStr.append("\n");
					}
					logOut.setText(newStr.toString());
				}

				if (!userScroll) {
					ScrollView sv = findViewById(R.id.sv_rcvdData);
					sv.post(new Runnable() {
						public void run() {
							sv.fullScroll(View.FOCUS_DOWN);
						}
					});
				}
			}

		}
	};

	private static IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BROADCAST_UART_RX);
		return intentFilter;
	}

	/**
	 * Callback of CONNECT/DISCONNECT button on DfuActivity
	 */
	public void onConnectClicked(final View view) {
		if (isBLEEnabled()) {
			if (mService == null) {
				setDefaultUI();
				showDeviceScanningDialog();
			} else {
				mService.disconnect();
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
