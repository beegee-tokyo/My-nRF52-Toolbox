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
package tk.giesecke.my_nrf52_tb.dfu;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.snackbar.Snackbar;

import no.nordicsemi.android.dfu.BuildConfig;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;
import tk.giesecke.my_nrf52_tb.AppHelpFragment;
import tk.giesecke.my_nrf52_tb.FeaturesActivity;
import tk.giesecke.my_nrf52_tb.R;
import tk.giesecke.my_nrf52_tb.SettingsActivity;
import tk.giesecke.my_nrf52_tb.dfu.adapter.FileBrowserAppsAdapter;
import tk.giesecke.my_nrf52_tb.dfu.fragment.UploadCancelFragment;
import tk.giesecke.my_nrf52_tb.scanner.ScannerFragment;

import static tk.giesecke.my_nrf52_tb.FeaturesActivity.reqUUID;

/**
 * DfuActivity is the main DFU activity It implements DFUManagerCallbacks to receive callbacks from DFUManager class It implements
 * DeviceScannerFragment.OnDeviceSelectedListener callback to receive callback when device is selected from scanning dialog The activity supports portrait and
 * landscape orientations
 */
//public class DfuActivity extends AppCompatActivity implements LoaderCallbacks<Cursor>, ScannerFragment.OnDeviceSelectedListener,
//		UploadCancelFragment.CancelFragmentListener, PermissionRationaleFragment.PermissionDialogListener {
@SuppressWarnings("deprecation")
public class DfuActivity extends AppCompatActivity implements LoaderCallbacks<Cursor>,
		ScannerFragment.OnDeviceSelectedListener,
		UploadCancelFragment.CancelFragmentListener,
		SharedPreferences.OnSharedPreferenceChangeListener {
	private static final String TAG = "DfuActivity";

	private static final String PREFS_DEVICE_NAME = "no.nordicsemi.android.nrftoolbox.dfu.PREFS_DEVICE_NAME";
	private static final String PREFS_FILE_NAME = "no.nordicsemi.android.nrftoolbox.dfu.PREFS_FILE_NAME";
	private static final String PREFS_FILE_TYPE = "no.nordicsemi.android.nrftoolbox.dfu.PREFS_FILE_TYPE";
	private static final String PREFS_FILE_SCOPE = "no.nordicsemi.android.nrftoolbox.dfu.PREFS_FILE_SCOPE";
	private static final String PREFS_FILE_SIZE = "no.nordicsemi.android.nrftoolbox.dfu.PREFS_FILE_SIZE";

	private static final String DATA_DEVICE = "device";
	private static final String DATA_FILE_TYPE = "file_type";
	private static final String DATA_FILE_TYPE_TMP = "file_type_tmp";
	private static final String DATA_FILE_PATH = "file_path";
	private static final String DATA_FILE_STREAM = "file_stream";
	private static final String DATA_INIT_FILE_PATH = "init_file_path";
	private static final String DATA_INIT_FILE_STREAM = "init_file_stream";
	private static final String DATA_STATUS = "status";
	private static final String DATA_SCOPE = "scope";
	private static final String DATA_DFU_COMPLETED = "dfu_completed";
	private static final String DATA_DFU_ERROR = "dfu_error";

	private static final String EXTRA_URI = "uri";

	private static final int ENABLE_BT_REQ = 0;
	private static final int SELECT_FILE_REQ = 1;
	private static final int SELECT_INIT_FILE_REQ = 2;

	private TextView mDeviceNameView;
	private TextView mFileNameView;
	private TextView mFileTypeView;
	private TextView mFileScopeView;
	private TextView mFileSizeView;
	private TextView mFileStatusView;
	private TextView mTextPercentage;
	private TextView mTextUploading;
	private ProgressBar mProgressBar;

	private Button mSelectFileButton, mUploadButton, mConnectButton;

	private BluetoothDevice mSelectedDevice;
	private String mFilePath;
	private Uri mFileStreamUri;
	private String mInitFilePath;
	private Uri mInitFileStreamUri;
	private int mFileType;
	private int mFileTypeTmp; // This value is being used when user is selecting a file not to overwrite the old value (in case he/she will cancel selecting file)
	private Integer mScope;
	private boolean mStatusOk;
	/** Flag set to true in {@link #onRestart()} and to false in {@link #onPause()}. */
	private boolean mResumed;
	/** Flag set to true if DFU operation was completed while {@link #mResumed} was false. */
	private boolean mDfuCompleted;
	/** The error message received from DFU service while {@link #mResumed} was false. */
	private String mDfuError;

	/**
	 * The progress listener receives events from the DFU Service.
	 * If is registered in onCreate() and unregistered in onDestroy() so methods here may also be called
	 * when the screen is locked or the app went to the background. This is because the UI needs to have the
	 * correct information after user comes back to the activity and this information can't be read from the service
	 * as it might have been killed already (DFU completed or finished with error).
	 */
	private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
		@Override
		public void onDeviceConnecting(final String deviceAddress) {
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_connecting);
		}

		@Override
		public void onDfuProcessStarting(final String deviceAddress) {
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_starting);
		}

		@Override
		public void onEnablingDfuMode(final String deviceAddress) {
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_switching_to_dfu);
		}

		@Override
		public void onFirmwareValidating(final String deviceAddress) {
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_validating);
		}

		@Override
		public void onDeviceDisconnecting(final String deviceAddress) {
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_disconnecting);
		}

		@Override
		public void onDfuCompleted(final String deviceAddress) {
			mTextPercentage.setText(R.string.dfu_status_completed);
			if (mResumed) {
				// let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
				new Handler().postDelayed(() -> {
					onTransferCompleted();

					// if this activity is still open and upload process was completed, cancel the notification
					final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					manager.cancel(DfuService.NOTIFICATION_ID);
				}, 200);
			} else {
				// Save that the DFU process has finished
				mDfuCompleted = true;
			}
		}

		@Override
		public void onDfuAborted(final String deviceAddress) {
			mTextPercentage.setText(R.string.dfu_status_aborted);
			// let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
			new Handler().postDelayed(() -> {
				onUploadCanceled();

				// if this activity is still open and upload process was completed, cancel the notification
				final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				manager.cancel(DfuService.NOTIFICATION_ID);
			}, 200);
		}

		@Override
		public void onProgressChanged(final String deviceAddress, final int percent, final float speed, final float avgSpeed, final int currentPart, final int partsTotal) {
			mProgressBar.setIndeterminate(false);
			mProgressBar.setProgress(percent);
			mTextPercentage.setText(getString(R.string.dfu_uploading_percentage, percent));
			if (partsTotal > 1)
				mTextUploading.setText(getString(R.string.dfu_status_uploading_part, currentPart, partsTotal));
			else
				mTextUploading.setText(R.string.dfu_status_uploading);
		}

		@Override
		public void onError(final String deviceAddress, final int error, final int errorType, final String message) {
			if (mResumed) {
				showErrorMessage(message);

				// We have to wait a bit before canceling notification. This is called before DfuService creates the last notification.
				new Handler().postDelayed(() -> {
					// if this activity is still open and upload process was completed, cancel the notification
					final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					manager.cancel(DfuService.NOTIFICATION_ID);
				}, 200);
			} else {
				mDfuError = message;
			}
		}
	};

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feature_dfu);
		isBLESupported();
		if (!isBLEEnabled()) {
			showBLEDialog();
		}

		ArrayList<String> arrPerm = new ArrayList<>();
		// On newer Android versions it is required to get the permission of the user to
		// get the location of the device. I am not sure at all what that has to be with
		// the permission to use Bluetooth or BLE, but you need to get it anyway
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			arrPerm.add(Manifest.permission.ACCESS_COARSE_LOCATION);
		}
		// On newer Android versions it is required to get the permission of the user to
		// access the storage of the device.
		if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			arrPerm.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}
		// On newer Android versions it is required to get the permission of the user to
		// access the storage of the device.
		if(ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				arrPerm.add(Manifest.permission.FOREGROUND_SERVICE);
			}
		}
		// On newer Android versions it is required to get the permission of the user to
		// access the storage of the device.
		if(ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
			arrPerm.add(Manifest.permission.INTERNET);
		}

		if(!arrPerm.isEmpty()) {
			String[] permissions = new String[arrPerm.size()];
			permissions = arrPerm.toArray(permissions);
			ActivityCompat.requestPermissions(this, permissions, 0);
		}

		setGUI();

		// restore saved state
		mFileType = DfuService.TYPE_AUTO; // Default
		if (savedInstanceState != null) {
			mFileType = savedInstanceState.getInt(DATA_FILE_TYPE);
			mFileTypeTmp = savedInstanceState.getInt(DATA_FILE_TYPE_TMP);
			mFilePath = savedInstanceState.getString(DATA_FILE_PATH);
			mFileStreamUri = savedInstanceState.getParcelable(DATA_FILE_STREAM);
			mInitFilePath = savedInstanceState.getString(DATA_INIT_FILE_PATH);
			mInitFileStreamUri = savedInstanceState.getParcelable(DATA_INIT_FILE_STREAM);
			mSelectedDevice = savedInstanceState.getParcelable(DATA_DEVICE);
			mStatusOk = mStatusOk || savedInstanceState.getBoolean(DATA_STATUS);
			mScope = savedInstanceState.containsKey(DATA_SCOPE) ? savedInstanceState.getInt(DATA_SCOPE) : null;
			mUploadButton.setEnabled(mSelectedDevice != null && mStatusOk);
			mDfuCompleted = savedInstanceState.getBoolean(DATA_DFU_COMPLETED);
			mDfuError = savedInstanceState.getString(DATA_DFU_ERROR);
		}

		DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(DATA_FILE_TYPE, mFileType);
		outState.putInt(DATA_FILE_TYPE_TMP, mFileTypeTmp);
		outState.putString(DATA_FILE_PATH, mFilePath);
		outState.putParcelable(DATA_FILE_STREAM, mFileStreamUri);
		outState.putString(DATA_INIT_FILE_PATH, mInitFilePath);
		outState.putParcelable(DATA_INIT_FILE_STREAM, mInitFileStreamUri);
		outState.putParcelable(DATA_DEVICE, mSelectedDevice);
		outState.putBoolean(DATA_STATUS, mStatusOk);
		if (mScope != null) outState.putInt(DATA_SCOPE, mScope);
		outState.putBoolean(DATA_DFU_COMPLETED, mDfuCompleted);
		outState.putString(DATA_DFU_ERROR, mDfuError);
	}

	private void setGUI() {
		final Toolbar toolbar = findViewById(R.id.toolbar_actionbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mDeviceNameView = findViewById(R.id.device_name);
		mFileNameView = findViewById(R.id.file_name);
		mFileTypeView = findViewById(R.id.file_type);
		mFileScopeView = findViewById(R.id.file_scope);
		mFileSizeView = findViewById(R.id.file_size);
		mFileStatusView = findViewById(R.id.file_status);
		mSelectFileButton = findViewById(R.id.action_select_file);
		mUploadButton = findViewById(R.id.action_upload);
		mConnectButton = findViewById(R.id.action_connect);
		mTextPercentage = findViewById(R.id.textviewProgress);
		mTextUploading = findViewById(R.id.textviewUploading);
		mProgressBar = findViewById(R.id.progressbar_file);

		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (isDfuServiceRunning()) {
			// Restore image file information
			mDeviceNameView.setText(preferences.getString(PREFS_DEVICE_NAME, ""));
			mFileNameView.setText(preferences.getString(PREFS_FILE_NAME, ""));
			mFileTypeView.setText(preferences.getString(PREFS_FILE_TYPE, ""));
			mFileScopeView.setText(preferences.getString(PREFS_FILE_SCOPE, ""));
			mFileSizeView.setText(preferences.getString(PREFS_FILE_SIZE, ""));
			mFileStatusView.setText(R.string.dfu_file_status_ok);
			mStatusOk = true;
			showProgressBar();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mResumed = true;
		if (mDfuCompleted)
			onTransferCompleted();
		if (mDfuError != null)
			showErrorMessage(mDfuError);
		if (mDfuCompleted || mDfuError != null) {
			// if this activity is still open and upload process was completed, cancel the notification
			final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			manager.cancel(DfuService.NOTIFICATION_ID);
			mDfuCompleted = false;
			mDfuError = null;
		}
		TextView libVersionView = findViewById(R.id.libVersion);
		String libVersion = getString(R.string.dfu_lib_version) + " " + BuildConfig.VERSION_NAME;
		libVersionView.setText(libVersion);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mResumed = false;
	}

	private void isBLESupported() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			showToast(R.string.no_ble);
			finish();
		}
	}

	private boolean isBLEEnabled() {
		final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		final BluetoothAdapter adapter = manager.getAdapter();
		return adapter != null && adapter.isEnabled();
	}

	private void showBLEDialog() {
		final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableIntent, ENABLE_BT_REQ);
	}

	private void showDeviceScanningDialog() {
		// UUID was NULL
		ScannerFragment dialog;
		if (reqUUID != null) {
			dialog = ScannerFragment.getInstance(reqUUID);
		}
		else {
			dialog = ScannerFragment.getInstance(null);
		}
//		final ScannerFragment dialog = ScannerFragment.getInstance(MY_SERVICE_UUID); // Device that is advertising directly does not have the GENERAL_DISCOVERABLE nor LIMITED_DISCOVERABLE flag set.
		dialog.show(getSupportFragmentManager(), "scan_fragment");
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.dfu_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				break;
			case R.id.action_about:
				final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.dfu_about_text);
				fragment.show(getSupportFragmentManager(), "help_fragment");
				break;
			case R.id.action_settings:
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
		}
		return true;
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != RESULT_OK)
			return;

		switch (requestCode) {
			case SELECT_FILE_REQ: {
				// clear previous data
				mFileType = mFileTypeTmp;
				mFilePath = null;
				mFileStreamUri = null;

				// and read new one
				final Uri uri = data.getData();
				/*
				 * The URI returned from application may be in 'file' or 'content' schema. 'File' schema allows us to create a File object and read details from if
				 * directly. Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
				 */
				if (uri.getScheme().equals("file")) {
					// the direct path to the file has been returned
					final String path = uri.getPath();
					final File file = new File(path);
					mFilePath = path;

					updateFileInfo(file.getName(), file.length(), mFileType);
				} else if (uri.getScheme().equals("content")) {
					// an Uri has been returned
					mFileStreamUri = uri;
					// if application returned Uri for streaming, let's us it. Does it works?
					// FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
					final Bundle extras = data.getExtras();
					if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
						mFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);

					// file name and size must be obtained from Content Provider
					final Bundle bundle = new Bundle();
					bundle.putParcelable(EXTRA_URI, uri);
					getLoaderManager().restartLoader(SELECT_FILE_REQ, bundle, this);
				}
				break;
			}
			case SELECT_INIT_FILE_REQ: {
				mInitFilePath = null;
				mInitFileStreamUri = null;

				// and read new one
				final Uri uri = data.getData();
				/*
				 * The URI returned from application may be in 'file' or 'content' schema. 'File' schema allows us to create a File object and read details from if
				 * directly. Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
				 */
				if (uri.getScheme().equals("file")) {
					// the direct path to the file has been returned
					mInitFilePath = uri.getPath();
					mFileStatusView.setText(R.string.dfu_file_status_ok_with_init);
				} else if (uri.getScheme().equals("content")) {
					// an Uri has been returned
					mInitFileStreamUri = uri;
					// if application returned Uri for streaming, let's us it. Does it works?
					// FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
					final Bundle extras = data.getExtras();
					if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
						mInitFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);
					mFileStatusView.setText(R.string.dfu_file_status_ok_with_init);
				}
				break;
			}
			default:
				break;
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		final Uri uri = args.getParcelable(EXTRA_URI);
		/*
		 * Some apps, f.e. Google Drive allow to select file that is not on the device. There is no "_data" column handled by that provider. Let's try to obtain
		 * all columns and than check which columns are present.
		 */
		// final String[] projection = new String[] { MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA };
		return new CursorLoader(this, uri, null /* all columns, instead of projection */, null, null, null);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		mFileNameView.setText(null);
		mFileTypeView.setText(null);
		mFileSizeView.setText(null);
		mFilePath = null;
		mFileStreamUri = null;
		mStatusOk = false;
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		if (data != null && data.moveToNext()) {
			/*
			 * Here we have to check the column indexes by name as we have requested for all. The order may be different.
			 */
			final String fileName = data.getString(data.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)/* 0 DISPLAY_NAME */);
			final int fileSize = data.getInt(data.getColumnIndex(MediaStore.MediaColumns.SIZE) /* 1 SIZE */);
			String filePath = null;
			final int dataIndex = data.getColumnIndex(MediaStore.MediaColumns.DATA);
			if (dataIndex != -1)
				filePath = data.getString(dataIndex /* 2 DATA */);
			if (!TextUtils.isEmpty(filePath))
				mFilePath = filePath;

			updateFileInfo(fileName, fileSize, mFileType);
		} else {
			mFileNameView.setText(null);
			mFileTypeView.setText(null);
			mFileSizeView.setText(null);
			mFilePath = null;
			mFileStreamUri = null;
			mFileStatusView.setText(R.string.dfu_file_status_error);
			mStatusOk = false;
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

	/**
	 * Updates the file information on UI
	 *
	 * @param fileName file name
	 * @param fileSize file length
	 */
	private void updateFileInfo(final String fileName, final long fileSize, final int fileType) {
		mFileNameView.setText(fileName);
		switch (fileType) {
			case DfuService.TYPE_AUTO:
				mFileTypeView.setText(getResources().getStringArray(R.array.dfu_file_type)[0]);
				break;
			case DfuService.TYPE_SOFT_DEVICE:
				mFileTypeView.setText(getResources().getStringArray(R.array.dfu_file_type)[1]);
				break;
			case DfuService.TYPE_BOOTLOADER:
				mFileTypeView.setText(getResources().getStringArray(R.array.dfu_file_type)[2]);
				break;
			case DfuService.TYPE_APPLICATION:
				mFileTypeView.setText(getResources().getStringArray(R.array.dfu_file_type)[3]);
				break;
		}
		mFileSizeView.setText(getString(R.string.dfu_file_size_text, fileSize));
		mFileScopeView.setText(getString(R.string.not_available));
		final String extension = mFileType == DfuService.TYPE_AUTO ? "(?i)ZIP" : "(?i)HEX|BIN"; // (?i) =  case insensitive
		final boolean statusOk = mStatusOk = MimeTypeMap.getFileExtensionFromUrl(fileName).matches(extension);
		mFileStatusView.setText(statusOk ? R.string.dfu_file_status_ok : R.string.dfu_file_status_invalid);
		mUploadButton.setEnabled(mSelectedDevice != null && statusOk);

		// Ask the user for the Init packet file if HEX or BIN files are selected. In case of a ZIP file the Init packets should be included in the ZIP.
		if (statusOk) {
			// TODO we are skipping the selection part here
			mScope = null;
			mFileScopeView.setText(getResources().getStringArray(R.array.dfu_file_scope)[0]);

//			if (fileType != DfuService.TYPE_AUTO) {
//				mFileScopeView.setText(getString(R.string.not_available));
//				new AlertDialog.Builder(this).setTitle(R.string.dfu_file_init_title).setMessage(R.string.dfu_file_init_message)
//						.setNegativeButton(R.string.no, (dialog, which) -> {
//							mInitFilePath = null;
//							mInitFileStreamUri = null;
//						}).setPositiveButton(R.string.yes, (dialog, which) -> {
//					final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//					intent.setType(DfuService.MIME_TYPE_OCTET_STREAM);
//					intent.addCategory(Intent.CATEGORY_OPENABLE);
//					startActivityForResult(intent, SELECT_INIT_FILE_REQ);
//				}).show();
//			} else {
//			    mScope = null;
//				new AlertDialog.Builder(this).setTitle(R.string.dfu_file_scope_title).setCancelable(false)
//						.setSingleChoiceItems(R.array.dfu_file_scope, 0, (dialog, which) -> {
//							switch (which) {
//								case 0:
//									mScope = null;
//									break;
//								case 1:
//									mScope = DfuServiceInitiator.SCOPE_SYSTEM_COMPONENTS;
//									break;
//								case 2:
//									mScope = DfuServiceInitiator.SCOPE_APPLICATION;
//									break;
//							}
//						})
//                        .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
//					int index;
//					if (mScope == null) {
//						index = 0;
//					} else if (mScope == DfuServiceInitiator.SCOPE_SYSTEM_COMPONENTS) {
//						index = 1;
//					} else {
//						index = 2;
//					}
//					mFileScopeView.setText(getResources().getStringArray(R.array.dfu_file_scope)[index]);
//				}).show();
//			}
		}
	}

	/**
	 * Called when the question mark was pressed
	 *
	 * @param view a button that was pressed
	 */
	public void onSelectFileHelpClicked(final View view) {
		new AlertDialog.Builder(this).setTitle(R.string.dfu_help_title).setMessage(R.string.dfu_help_message).setPositiveButton(R.string.ok, null)
				.show();
	}

	/**
	 * Called when Select File was pressed
	 *
	 * @param view a button that was pressed
	 */
	public void onSelectFileClicked(final View view) {
		mFileTypeTmp = mFileType;
		mFileTypeTmp = DfuService.TYPE_AUTO;
		openFileChooser();

		// TODO we are skipping the selection part here
//		int index = 0;
//		switch (mFileType) {
//			case DfuService.TYPE_AUTO:
//				index = 0;
//				break;
//			case DfuService.TYPE_SOFT_DEVICE:
//				index = 1;
//				break;
//			case DfuService.TYPE_BOOTLOADER:
//				index = 2;
//				break;
//			case DfuService.TYPE_APPLICATION:
//				index = 3;
//				break;
//		}
//		// Show a dialog with file types
//		new AlertDialog.Builder(this).setTitle(R.string.dfu_file_type_title)
//				.setSingleChoiceItems(R.array.dfu_file_type, index, (dialog, which) -> {
//					switch (which) {
//						case 0:
//							mFileTypeTmp = DfuService.TYPE_AUTO;
//							break;
//						case 1:
//							mFileTypeTmp = DfuService.TYPE_SOFT_DEVICE;
//							break;
//						case 2:
//							mFileTypeTmp = DfuService.TYPE_BOOTLOADER;
//							break;
//						case 3:
//							mFileTypeTmp = DfuService.TYPE_APPLICATION;
//							break;
//					}
//				})
//				.setPositiveButton(R.string.ok, (dialog, which) -> openFileChooser())
//                .setNeutralButton(R.string.dfu_file_info, (dialog, which) -> {
//			final ZipInfoFragment fragment = new ZipInfoFragment();
//			fragment.show(getSupportFragmentManager(), "help_fragment");
//		})
//				.setNegativeButton(R.string.cancel, null).show();
	}

	private void openFileChooser() {
		final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType(mFileTypeTmp == DfuService.TYPE_AUTO ? DfuService.MIME_TYPE_ZIP : DfuService.MIME_TYPE_OCTET_STREAM);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		if (intent.resolveActivity(getPackageManager()) != null) {
			// file browser has been found on the device
			startActivityForResult(intent, SELECT_FILE_REQ);
		} else {
			// there is no any file browser app, let's try to download one
			@SuppressLint("InflateParams") final View customView = getLayoutInflater().inflate(R.layout.app_file_browser, null);
			final ListView appsList = customView.findViewById(android.R.id.list);
			appsList.setAdapter(new FileBrowserAppsAdapter(this));
			appsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			appsList.setItemChecked(0, true);
			new AlertDialog.Builder(this).setTitle(R.string.dfu_alert_no_filebrowser_title).setView(customView)
					.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss()).setPositiveButton(R.string.ok, (dialog, which) -> {
				final int pos = appsList.getCheckedItemPosition();
				if (pos >= 0) {
					final String query = getResources().getStringArray(R.array.dfu_app_file_browser_action)[pos];
					final Intent storeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(query));
					startActivity(storeIntent);
				}
			}).show();
		}
	}

	/**
	 * Callback of UPDATE/CANCEL button on DfuActivity
	 */
	public void onUploadClicked(final View view) {
		if (isDfuServiceRunning()) {
			showUploadCancelDialog();
			return;
		}

		// Check whether the selected file is a HEX file (we are just checking the extension)
		if (!mStatusOk) {
			Toast.makeText(this, R.string.dfu_file_status_invalid_message, Toast.LENGTH_LONG).show();
			return;
		}

		// Save current state in order to restore it if user quit the Activity
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor editor = preferences.edit();
		editor.putString(PREFS_DEVICE_NAME, mSelectedDevice.getName());
		editor.putString(PREFS_FILE_NAME, mFileNameView.getText().toString());
		editor.putString(PREFS_FILE_TYPE, mFileTypeView.getText().toString());
		editor.putString(PREFS_FILE_SCOPE, mFileScopeView.getText().toString());
		editor.putString(PREFS_FILE_SIZE, mFileSizeView.getText().toString());
		editor.apply();

		showProgressBar();

		final DfuServiceInitiator starter = new DfuServiceInitiator(mSelectedDevice.getAddress())
				.setDeviceName(mSelectedDevice.getName())
				.setKeepBond(false)
				.setRestoreBond(false)
				.setForceDfu(true)
				.setPacketsReceiptNotificationsEnabled(true)
				.setPacketsReceiptNotificationsValue(3)
				.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
				.setForeground(true);
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
//			starter.setPacketsReceiptNotificationsValue(DfuServiceInitiator.DEFAULT_PRN_VALUE);
			starter.setPacketsReceiptNotificationsValue(10);
			Log.e(TAG, "Android SDK < 26 (8.0) set PRN to 10");
		} else {
			starter.setPacketsReceiptNotificationsValue(4);
			Log.e(TAG, "Android SDK >= 26 (8.0) set PRN to 4");
		}
		if (mFileType == DfuService.TYPE_AUTO) {
			starter.setZip(mFileStreamUri, mFilePath);
			if (mScope != null)
				starter.setScope(mScope);
		} else {
			starter.setBinOrHex(mFileType, mFileStreamUri, mFilePath).setInitFile(mInitFileStreamUri, mInitFilePath);
		}
		starter.start(this, DfuService.class);
	}

	private void showUploadCancelDialog() {
		final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
		final Intent pauseAction = new Intent(DfuService.BROADCAST_ACTION);
		pauseAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_PAUSE);
		manager.sendBroadcast(pauseAction);

		final UploadCancelFragment fragment = UploadCancelFragment.getInstance();
		fragment.show(getSupportFragmentManager(), TAG);
	}

	/**
	 * Callback of CONNECT/DISCONNECT button on DfuActivity
	 */
	public void onConnectClicked(final View view) {
		if (isBLEEnabled()) {
			showDeviceScanningDialog();
		} else {
			showBLEDialog();
		}
	}

	@Override
	public void onDeviceSelected(final BluetoothDevice device, final String name) {
		mSelectedDevice = device;
		mUploadButton.setEnabled(mStatusOk);
		mDeviceNameView.setText(name != null ? name : getString(R.string.not_available));
	}

	@Override
	public void onDialogCanceled() {
		// do nothing
	}

	private void showProgressBar() {
		mProgressBar.setVisibility(View.VISIBLE);
		mTextPercentage.setVisibility(View.VISIBLE);
		mTextPercentage.setText(null);
		mTextUploading.setText(R.string.dfu_status_uploading);
		mTextUploading.setVisibility(View.VISIBLE);
		mConnectButton.setEnabled(false);
		mSelectFileButton.setEnabled(false);
		mUploadButton.setEnabled(true);
		mUploadButton.setText(android.R.string.cancel);
	}

	private void onTransferCompleted() {
		clearUI(true);
//		showToast(R.string.dfu_success);
		Snackbar successMsg = Snackbar.make(findViewById(android.R.id.content), getString(R.string.dfu_success),
				Snackbar.LENGTH_INDEFINITE);
		successMsg.setAction("CLOSE", new View.OnClickListener() {
			@Override
			public void onClick(View view) {
			}
		});
		successMsg.setActionTextColor(getResources().getColor(android.R.color.holo_red_dark));
		View sbView = successMsg.getView();
		sbView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
		TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
		textView.setTextColor(getResources().getColor(android.R.color.black));

		successMsg.show();
	}

	public void onUploadCanceled() {
		clearUI(false);
		showToast(R.string.dfu_aborted);
	}

	@Override
	public void onCancelUpload() {
		mProgressBar.setIndeterminate(true);
		mTextUploading.setText(R.string.dfu_status_aborting);
		mTextPercentage.setText(null);
	}

	private void showErrorMessage(final String message) {
		clearUI(false);
		showToast("Upload failed: " + message);
		Snackbar failedMsg = Snackbar.make(findViewById(android.R.id.content), "Upload failed: " + message,
				Snackbar.LENGTH_INDEFINITE);
		failedMsg.setAction("CLOSE", new View.OnClickListener() {
			@Override
			public void onClick(View view) {
			}
		});
		failedMsg.setActionTextColor(getResources().getColor(android.R.color.black));
		View sbView = failedMsg.getView();
		sbView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
		TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
		textView.setTextColor(getResources().getColor(android.R.color.white));

		failedMsg.show();
	}

	private void clearUI(final boolean clearDevice) {
		mProgressBar.setVisibility(View.INVISIBLE);
		mTextPercentage.setVisibility(View.INVISIBLE);
		mTextUploading.setVisibility(View.INVISIBLE);
		mConnectButton.setEnabled(true);
		mSelectFileButton.setEnabled(true);
		mUploadButton.setEnabled(false);
		mUploadButton.setText(R.string.dfu_action_upload);
		if (clearDevice) {
			mSelectedDevice = null;
			mDeviceNameView.setText(R.string.dfu_default_name);
		}
		// Application may have lost the right to these files if Activity was closed during upload (grant uri permission). Clear file related values.
		mFileNameView.setText(null);
		mFileTypeView.setText(null);
		mFileScopeView.setText(null);
		mFileSizeView.setText(null);
		mFileStatusView.setText(R.string.dfu_file_status_no_file);
		mFilePath = null;
		mFileStreamUri = null;
		mInitFilePath = null;
		mInitFileStreamUri = null;
		mStatusOk = false;
	}

	private void showToast(final int messageResId) {
		Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
	}

	private void showToast(final String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	private boolean isDfuServiceRunning() {
		final ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (DfuService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
