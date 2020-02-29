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
package tk.giesecke.my_nrf52_tb.profile.multiconnect;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import tk.giesecke.my_nrf52_tb.AppHelpFragment;
import tk.giesecke.my_nrf52_tb.R;
import tk.giesecke.my_nrf52_tb.scanner.ScannerFragment;

/**
 * <p>
 * The {@link BleMulticonnectProfileServiceReadyActivity} activity is designed to be the base class for profile activities that uses services in order to connect
 * more than one device at the same time. A service extending {@link BleMulticonnectProfileService} is created when the activity is created, and the activity binds to it.
 * The service returns a binder that may be used to connect, disconnect or manage devices, and notifies the
 * activity using Local Broadcasts ({@link LocalBroadcastManager}). See {@link BleMulticonnectProfileService} for messages. If the device is not in range it will listen for
 * it and connect when it become visible. The service exists until all managed devices have been disconnected and unmanaged and the last activity unbinds from it.
 * </p>
 * <p>
 * When user closes the activity (e.g. by pressing Back button) while being connected, the Service remains working. It's remains connected to the devices or still
 * listens for updates from them. When entering back to the activity, activity will to bind to the service and refresh UI.
 * </p>
 */
@SuppressWarnings("deprecation")
public abstract class BleMulticonnectProfileServiceReadyActivity<E extends BleMulticonnectProfileService.LocalBinder> extends AppCompatActivity implements
        ScannerFragment.OnDeviceSelectedListener, BleManagerCallbacks {
	private static final String TAG = "BleMulticonnectProfileServiceReadyActivity";

	protected static final int REQUEST_ENABLE_BT = 2;

	private E mService;
	private List<BluetoothDevice> mManagedDevices;

	private final BroadcastReceiver mCommonBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BleMulticonnectProfileService.EXTRA_DEVICE);
			final String action = intent.getAction();
			switch (action) {
				case BleMulticonnectProfileService.BROADCAST_CONNECTION_STATE: {
					final int state = intent.getIntExtra(BleMulticonnectProfileService.EXTRA_CONNECTION_STATE, BleMulticonnectProfileService.STATE_DISCONNECTED);

					switch (state) {
						case BleMulticonnectProfileService.STATE_CONNECTED: {
							onDeviceConnected(bluetoothDevice);
							break;
						}
						case BleMulticonnectProfileService.STATE_DISCONNECTED: {
							onDeviceDisconnected(bluetoothDevice);
							break;
						}
						case BleMulticonnectProfileService.STATE_LINK_LOSS: {
							onLinkLossOccurred(bluetoothDevice);
							break;
						}
						case BleMulticonnectProfileService.STATE_CONNECTING: {
							onDeviceConnecting(bluetoothDevice);
							break;
						}
						case BleMulticonnectProfileService.STATE_DISCONNECTING: {
							onDeviceDisconnecting(bluetoothDevice);
							break;
						}
						default:
							// there should be no other actions
							break;
					}
					break;
				}
				case BleMulticonnectProfileService.BROADCAST_SERVICES_DISCOVERED: {
					final boolean primaryService = intent.getBooleanExtra(BleMulticonnectProfileService.EXTRA_SERVICE_PRIMARY, false);
					final boolean secondaryService = intent.getBooleanExtra(BleMulticonnectProfileService.EXTRA_SERVICE_SECONDARY, false);

					if (primaryService) {
						onServicesDiscovered(bluetoothDevice, secondaryService);
					} else {
						onDeviceNotSupported(bluetoothDevice);
					}
					break;
				}
				case BleMulticonnectProfileService.BROADCAST_DEVICE_READY: {
					onDeviceReady(bluetoothDevice);
					break;
				}
				case BleMulticonnectProfileService.BROADCAST_BOND_STATE: {
					final int state = intent.getIntExtra(BleMulticonnectProfileService.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
					switch (state) {
						case BluetoothDevice.BOND_BONDING:
							onBondingRequired(bluetoothDevice);
							break;
						case BluetoothDevice.BOND_BONDED:
							onBonded(bluetoothDevice);
							break;
					}
					break;
				}
				case BleMulticonnectProfileService.BROADCAST_BATTERY_LEVEL: {
					final int value = intent.getIntExtra(BleMulticonnectProfileService.EXTRA_BATTERY_LEVEL, -1);
					if (value > 0)
						onBatteryValueReceived(bluetoothDevice, value);
					break;
				}
				case BleMulticonnectProfileService.BROADCAST_ERROR: {
					final String message = intent.getStringExtra(BleMulticonnectProfileService.EXTRA_ERROR_MESSAGE);
					final int errorCode = intent.getIntExtra(BleMulticonnectProfileService.EXTRA_ERROR_CODE, 0);
					onError(bluetoothDevice, message, errorCode);
					break;
				}
			}
		}
	};

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@SuppressWarnings("unchecked")
		@Override
		public void onServiceConnected(final ComponentName name, final IBinder service) {
			final E bleService = mService = (E) service;
			mManagedDevices.addAll(bleService.getManagedDevices());
			onServiceBound(bleService);

			// and notify user if device is connected
			for (final BluetoothDevice device : mManagedDevices) {
				if (bleService.isConnected(device))
					onDeviceConnected(device);
			}
		}

		@Override
		public void onServiceDisconnected(final ComponentName name) {
			mService = null;
			onServiceUnbound();
		}
	};

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mManagedDevices = new ArrayList<>();

		ensureBLESupported();
		if (!isBLEEnabled()) {
			showBLEDialog();
		}

		// In onInitialize method a final class may register local broadcast receivers that will listen for events from the service
		onInitialize(savedInstanceState);
		// The onCreateView class should... create the view
		onCreateView(savedInstanceState);

        final Toolbar toolbar = findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

		// Common nRF Toolbox view references are obtained here
		setUpView();
		// View is ready to be used
		onViewCreated(savedInstanceState);

		LocalBroadcastManager.getInstance(this).registerReceiver(mCommonBroadcastReceiver, makeIntentFilter());
	}

	@Override
	protected void onStart() {
		super.onStart();

		/*
		 * In comparison to BleProfileServiceReadyActivity this activity always starts the service when started.
		 * Connecting to a device is done by calling mService.connect(BluetoothDevice) method, not startService(...) like there.
		 * The service will stop itself when all devices it manages were disconnected and unmanaged and the last activity unbinds from it.
		 */
		final Intent service = new Intent(this, getServiceClass());
		startService(service);
		bindService(service, mServiceConnection, 0);
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (mService != null) {
			// We don't want to perform some operations (e.g. disable Battery Level notifications) in the service if we are just rotating the screen.
			// However, when the activity will disappear, we may want to disable some device features to reduce the battery consumption.
			mService.setActivityIsChangingConfiguration(isChangingConfigurations());
		}

		unbindService(mServiceConnection);
		mService = null;

		onServiceUnbound();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(mCommonBroadcastReceiver);
	}

	private static IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BleMulticonnectProfileService.BROADCAST_CONNECTION_STATE);
		intentFilter.addAction(BleMulticonnectProfileService.BROADCAST_SERVICES_DISCOVERED);
		intentFilter.addAction(BleMulticonnectProfileService.BROADCAST_DEVICE_READY);
		intentFilter.addAction(BleMulticonnectProfileService.BROADCAST_BOND_STATE);
		intentFilter.addAction(BleMulticonnectProfileService.BROADCAST_BATTERY_LEVEL);
		intentFilter.addAction(BleMulticonnectProfileService.BROADCAST_ERROR);
		return intentFilter;
	}

	/**
	 * Called when activity binds to the service. The parameter is the object returned in {@link Service#onBind(Intent)} method in your service.
	 * It is safe to obtain managed devices now.
	 */
	protected abstract void onServiceBound(E binder);

	/**
	 * Called when activity unbinds from the service. You may no longer use this binder methods.
	 */
	protected abstract void onServiceUnbound();

	/**
	 * Returns the service class for sensor communication. The service class must derive from {@link BleMulticonnectProfileService} in order to operate with this class.
	 *
	 * @return the service class
	 */
	protected abstract Class<? extends BleMulticonnectProfileService> getServiceClass();

	/**
	 * Returns the service interface that may be used to communicate with the sensor. This will return <code>null</code> if the device is disconnected from the
	 * sensor.
	 *
	 * @return the service binder or <code>null</code>
	 */
	protected E getService() {
		return mService;
	}

	/**
	 * You may do some initialization here. This method is called from {@link #onCreate(Bundle)} before the view was created.
	 */
	protected void onInitialize(final Bundle savedInstanceState) {
		// empty default implementation
	}

	/**
	 * Called from {@link #onCreate(Bundle)}. This method should build the activity UI, i.e. using {@link #setContentView(int)}.
	 * Use to obtain references to views. Connect/Disconnect button and the device name view are manager automatically.
	 *
	 * @param savedInstanceState contains the data it most recently supplied in {@link #onSaveInstanceState(Bundle)}.
	 *                           Note: <b>Otherwise it is null</b>.
	 */
	protected abstract void onCreateView(final Bundle savedInstanceState);

	/**
	 * Called after the view has been created.
	 *
	 * @param savedInstanceState contains the data it most recently supplied in {@link #onSaveInstanceState(Bundle)}.
	 *                           Note: <b>Otherwise it is null</b>.
	 */
	protected void onViewCreated(final Bundle savedInstanceState) {
		// empty default implementation
	}

	/**
	 * Called after the view and the toolbar has been created.
	 */
	protected final void setUpView() {
		// set GUI
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.help, menu);
		return true;
	}

	/**
	 * Use this method to handle menu actions other than home and about.
	 *
	 * @param itemId the menu item id
	 * @return <code>true</code> if action has been handled
	 */
	protected boolean onOptionsItemSelected(final int itemId) {
		// Overwrite when using menu other than R.menu.help
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		final int id = item.getItemId();
		switch (id) {
			case android.R.id.home:
				onBackPressed();
				break;
			case R.id.action_about:
				final AppHelpFragment fragment = AppHelpFragment.getInstance(getAboutTextId());
				fragment.show(getSupportFragmentManager(), "help_fragment");
				break;
			default:
				return onOptionsItemSelected(id);
		}
		return true;
	}

	/**
	 * Called when user press ADD DEVICE button. See layout files -> onClick attribute.
	 */
	public void onAddDeviceClicked(final View view) {
		if (isBLEEnabled()) {
			showDeviceScanningDialog(getFilterUUID());
		} else {
			showBLEDialog();
		}
	}

	@Override
	public void onDeviceSelected(final BluetoothDevice device, final String name) {
		mService.connect(device);
	}

	@Override
	public void onDialogCanceled() {
		// do nothing
	}

	@Override
	public void onDeviceConnecting(final BluetoothDevice device) {
		// empty default implementation
	}

	@Override
	public void onDeviceDisconnecting(final BluetoothDevice device) {
		// empty default implementation
	}

	@Override
	public void onLinkLossOccurred(final BluetoothDevice device) {
		// empty default implementation
	}

	@Override
	public void onServicesDiscovered(final BluetoothDevice device, final boolean optionalServicesFound) {
		// empty default implementation
	}

	@Override
	public void onDeviceReady(final BluetoothDevice device) {
		// empty default implementation
	}

	@Override
	public void onBondingRequired(final BluetoothDevice device) {
		// empty default implementation
	}

	@Override
	public void onBonded(final BluetoothDevice device) {
		// empty default implementation
	}

	@Override
	public void onBondingFailed(final BluetoothDevice device) {
		// empty default implementation
	}

	@Override
	public void onDeviceNotSupported(final BluetoothDevice device) {
		showToast(R.string.not_supported);
	}

	@Override
	public final boolean shouldEnableBatteryLevelNotifications(final BluetoothDevice device) {
		// This method will never be called.
		// Please see BleMulticonnectProfileService#shouldEnableBatteryLevelNotifications(BluetoothDevice) instead.
		throw new UnsupportedOperationException("This method should not be called");
	}

	@Override
	public void onBatteryValueReceived(final BluetoothDevice device, final int value) {
		// empty default implementation
	}

	@Override
	public void onError(final BluetoothDevice device, final String message, final int errorCode) {
		showToast(message + " (" + errorCode + ")");
	}

	/**
	 * Shows a message as a Toast notification. This method is thread safe, you can call it from any thread
	 *
	 * @param message a message to be shown
	 */
	protected void showToast(final String message) {
		runOnUiThread(() -> Toast.makeText(BleMulticonnectProfileServiceReadyActivity.this, message, Toast.LENGTH_LONG).show());
	}

	/**
	 * Shows a message as a Toast notification. This method is thread safe, you can call it from any thread
	 *
	 * @param messageResId an resource id of the message to be shown
	 */
	protected void showToast(final int messageResId) {
		runOnUiThread(() -> Toast.makeText(BleMulticonnectProfileServiceReadyActivity.this, messageResId, Toast.LENGTH_SHORT).show());
	}

	/**
	 * Returns the string resource id that will be shown in About box
	 *
	 * @return the about resource id
	 */
	protected abstract int getAboutTextId();

	/**
	 * The UUID filter is used to filter out available devices that does not have such UUID in their advertisement packet. See also:
	 * {@link #isChangingConfigurations()}.
	 *
	 * @return the required UUID or <code>null</code>
	 */
	protected abstract UUID getFilterUUID();

	/**
	 * Returns unmodifiable list of managed devices. Managed device is a device the was selected on ScannerFragment until it's removed from the managed list.
	 * It does not have to be connected at that moment.
	 * @return unmodifiable list of managed devices
	 */
	protected List<BluetoothDevice> getManagedDevices() {
		return Collections.unmodifiableList(mManagedDevices);
	}

	/**
	 * Returns <code>true</code> if the device is connected. Services may not have been discovered yet.
	 * @param device the device to check if it's connected
	 */
	protected boolean isDeviceConnected(final BluetoothDevice device) {
		return mService != null && mService.isConnected(device);
	}

	/**
	 * Shows the scanner fragment.
	 *
	 * @param filter               the UUID filter used to filter out available devices. The fragment will always show all bonded devices as there is no information about their
	 *                             services
	 * @see #getFilterUUID()
	 */
	private void showDeviceScanningDialog(final UUID filter) {
		final ScannerFragment dialog = ScannerFragment.getInstance(filter);
		dialog.show(getSupportFragmentManager(), "scan_fragment");
	}

	private void ensureBLESupported() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
			finish();
		}
	}

	protected boolean isBLEEnabled() {
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		final BluetoothAdapter adapter = bluetoothManager.getAdapter();
		return adapter != null && adapter.isEnabled();
	}

	protected void showBLEDialog() {
		final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	}
}
