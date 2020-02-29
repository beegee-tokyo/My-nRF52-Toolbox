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
package tk.giesecke.my_nrf52_tb;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;

import tk.giesecke.my_nrf52_tb.adapter.AppAdapter;

public class FeaturesActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	private static final String NRF_CONNECT_CATEGORY = "no.nordicsemi.android.nrftoolbox.LAUNCHER";
	private static final String UTILS_CATEGORY = "no.nordicsemi.android.nrftoolbox.UTILS";
	private static final String NRF_CONNECT_PACKAGE = "no.nordicsemi.android.mcp";
	private static final String NRF_CONNECT_CLASS = NRF_CONNECT_PACKAGE + ".DeviceListActivity";
	private static final String NRF_CONNECT_MARKET_URI = "market://details?id=no.nordicsemi.android.mcp";

	// Extras that can be passed from NFC (see SplashscreenActivity)
	public static final String EXTRA_APP = "application/vnd.no.nordicsemi.type.app";
	public static final String EXTRA_ADDRESS = "application/vnd.no.nordicsemi.type.address";

//	private DrawerLayout mDrawerLayout;
//	private ActionBarDrawerToggle mDrawerToggle;

	/**
	 * Filter for device names
	 */
	public static String devicePrefix = "";
	/**
	 * Filter UUID for scan
	 */
	public static UUID reqUUID = null;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_features);

		final Toolbar toolbar = findViewById(R.id.toolbar_actionbar);
		setSupportActionBar(toolbar);

		// ensure that Bluetooth exists
		if (!ensureBLEExists())
			finish();

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		ArrayList<String> arrPerm = new ArrayList<>();
		// On newer Android versions it is required to get the permission of the user to
		// get the location of the device. I am not sure at all what that has to be with
		// the permission to use Bluetooth or BLE, but you need to get it anyway
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			arrPerm.add(Manifest.permission.ACCESS_COARSE_LOCATION);
		}
		// On newer Android versions it is required to get the permission of the user to
		// access the storage of the device.
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			arrPerm.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}
		// On newer Android versions it is required to get the permission of the user to
		// access the storage of the device.
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				arrPerm.add(Manifest.permission.FOREGROUND_SERVICE);
			}
		}
		// On newer Android versions it is required to get the permission of the user to
		// access the storage of the device.
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
			arrPerm.add(Manifest.permission.INTERNET);
		}
		// On newer Android versions it is required to get the permission of the user to
		// access WiFi status.
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
			arrPerm.add(Manifest.permission.ACCESS_WIFI_STATE);
		}
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
			arrPerm.add(Manifest.permission.CHANGE_WIFI_STATE);
		}
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			arrPerm.add(Manifest.permission.ACCESS_COARSE_LOCATION);
		}

		if (!arrPerm.isEmpty()) {
			String[] permissions = new String[arrPerm.size()];
			permissions = arrPerm.toArray(permissions);
			ActivityCompat.requestPermissions(this, permissions, 0);
		}

		// Enable access to internet
		// ThreadPolicy to get permission to access internet
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		//		final DrawerLayout drawer = mDrawerLayout = findViewById(R.id.drawer_layout);
//		drawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
//
//		// Set the drawer toggle as the DrawerListener
//		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
//            @Override
//            public void onDrawerSlide(final View drawerView, final float slideOffset) {
//                // Disable the Hamburger icon animation
//                super.onDrawerSlide(drawerView, 0);
//            }
//        };
//		drawer.addDrawerListener(mDrawerToggle);
//
//		// setup plug-ins in the drawer
//		setupPluginsInDrawer((ViewGroup) drawer.findViewById(R.id.plugin_container));

		// configure the app grid
		final GridView grid = findViewById(R.id.grid);
		grid.setAdapter(new AppAdapter(this));
		grid.setEmptyView(findViewById(android.R.id.empty));

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		final Intent intent = getIntent();
		if (intent.hasExtra(EXTRA_APP) && intent.hasExtra(EXTRA_ADDRESS)) {
			final String app = intent.getStringExtra(EXTRA_APP);
			switch (app) {
				default:
					// other are not supported yet
			}
		}

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);

		boolean uuidFilterEnabled = false;
		boolean nameFilterEnabled = false;

		if (sharedPreferences.getBoolean("uuid_filter", false)) {
			String tempUUID = sharedPreferences.getString(("uuid_filter_value"), "");
			boolean valid128UUID = false;
			boolean valid16UUID = false;
			if (Pattern.matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", tempUUID)) {
				valid128UUID = true;
			}
			if (Pattern.matches("[a-fA-F0-9]{4}", tempUUID)) {
				valid16UUID = true;
			}
			if (valid16UUID) {
				tempUUID = "0000" + tempUUID + "-0000-1000-8000-00805F9B34FB";
			}
			if (valid16UUID || valid128UUID) {
				try {
					reqUUID = UUID.fromString(tempUUID);
					uuidFilterEnabled = true;
				} catch (IllegalArgumentException ignore) {
					reqUUID = null;
				}
			}
		} else {
			reqUUID = null;
		}
		if (sharedPreferences.getBoolean("name_filter", false)) {
			devicePrefix = sharedPreferences.getString(("name_filter_value"), "");
			nameFilterEnabled = true;
		} else {
			devicePrefix = "";
		}

		if (uuidFilterEnabled || nameFilterEnabled) {
			String msg = "REMINDER: You have enabled ";
			if (uuidFilterEnabled && nameFilterEnabled) {
				msg += "both UUID and device name filters!";
			} else if (uuidFilterEnabled) {
				msg += "the UUID filter!";
			} else {
				msg += "the device name filter!";
			}
			Snackbar snackbar = Snackbar
					.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG);
			snackbar.show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.help, menu);
		return true;
	}

	@Override
	protected void onPostCreate(final Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
//		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
//		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
//		if (mDrawerToggle.onOptionsItemSelected(item)) {
//			return true;
//		}

		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				break;
			case R.id.action_about:
				final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.about_text, true);
				fragment.show(getSupportFragmentManager(), null);
				break;
			case R.id.action_settings:
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
		}
		return true;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		if (key.equals("uuid_filter") || key.equals("uuid_filter_value")) {
			if (sharedPreferences.getBoolean("uuid_filter", true)) {
				String newUuidFilter = sharedPreferences.getString(("uuid_filter_value"), "");
				if (newUuidFilter.isEmpty()) {
					return;
				}
				String tempUUID = sharedPreferences.getString(("uuid_filter_value"), "");
				boolean valid128UUID = false;
				boolean valid16UUID = false;
				if (Pattern.matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", tempUUID)) {
					valid128UUID = true;
				}
				if (Pattern.matches("[a-fA-F0-9]{4}", tempUUID)) {
					valid16UUID = true;
				}
				if (valid16UUID) {
					tempUUID = "0000" + tempUUID + "-0000-1000-8000-00805F9B34FB";
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
			if (sharedPreferences.getBoolean("name_filter", true)) {
				devicePrefix = sharedPreferences.getString(("name_filter_value"), "");
			} else {
				reqUUID = null;
			}
		}
	}

//	private void setupPluginsInDrawer(final ViewGroup container) {
//		final LayoutInflater inflater = LayoutInflater.from(this);
//		final PackageManager pm = getPackageManager();
//
//		// look for nRF Connect
//		final Intent nrfConnectIntent = new Intent(Intent.ACTION_MAIN);
//		nrfConnectIntent.addCategory(NRF_CONNECT_CATEGORY);
//		nrfConnectIntent.setClassName(NRF_CONNECT_PACKAGE, NRF_CONNECT_CLASS);
//		final ResolveInfo nrfConnectInfo = pm.resolveActivity(nrfConnectIntent, 0);
//
//		// configure link to nRF Connect
//		final TextView nrfConnectItem = container.findViewById(R.id.link_mcp);
//		if (nrfConnectInfo == null) {
//			nrfConnectItem.setTextColor(Color.GRAY);
//			ColorMatrix grayscale = new ColorMatrix();
//			grayscale.setSaturation(0.0f);
//			nrfConnectItem.getCompoundDrawables()[0].mutate().setColorFilter(new ColorMatrixColorFilter(grayscale));
//		}
//		nrfConnectItem.setOnClickListener(v -> {
//			Intent action = nrfConnectIntent;
//			if (nrfConnectInfo == null)
//				action = new Intent(Intent.ACTION_VIEW, Uri.parse(NRF_CONNECT_MARKET_URI));
//			action.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
//			action.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			try {
//				startActivity(action);
//			} catch (final ActivityNotFoundException e) {
//				Toast.makeText(FeaturesActivity.this, R.string.no_application_play, Toast.LENGTH_SHORT).show();
//			}
//			mDrawerLayout.closeDrawers();
//		});
//
//		// look for other plug-ins
//		final Intent utilsIntent = new Intent(Intent.ACTION_MAIN);
//		utilsIntent.addCategory(UTILS_CATEGORY);
//
//		final List<ResolveInfo> appList = pm.queryIntentActivities(utilsIntent, 0);
//		for (final ResolveInfo info : appList) {
//			final View item = inflater.inflate(R.layout.drawer_plugin, container, false);
//			final ImageView icon = item.findViewById(android.R.id.icon);
//			final TextView label = item.findViewById(android.R.id.text1);
//
//			label.setText(info.loadLabel(pm));
//			icon.setImageDrawable(info.loadIcon(pm));
//			item.setOnClickListener(v -> {
//				final Intent intent = new Intent();
//				intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
//				intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
//				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				startActivity(intent);
//				mDrawerLayout.closeDrawers();
//			});
//			container.addView(item);
//		}
//	}

	private boolean ensureBLEExists() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
}
