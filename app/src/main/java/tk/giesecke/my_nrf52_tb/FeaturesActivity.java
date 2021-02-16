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
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.Toast;

import java.util.ArrayList;

import tk.giesecke.my_nrf52_tb.adapter.AppAdapter;

public class FeaturesActivity extends AppCompatActivity {

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_features);

        final Toolbar toolbar = findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

		// ensure that Bluetooth exists
		if (!ensureBLEExists())
			finish();

		ArrayList<String> arrPerm = new ArrayList<>();
		// On newer Android versions it is required to get the permission of the user to
		// get the location of the device. I am not sure at all what that has to be with
		// the permission to use Bluetooth or BLE, but you need to get it anyway
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			arrPerm.add(Manifest.permission.ACCESS_FINE_LOCATION);
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
		// On newer Android versions it is required to get the permission of the user to
		// access the camera.
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			arrPerm.add(Manifest.permission.CAMERA);
		}
		if (!arrPerm.isEmpty()) {
			AlertDialog alertDialog1 = new AlertDialog.Builder(FeaturesActivity.this).create();

			// Setting Dialog Title
			alertDialog1.setTitle(R.string.scanner_permission_name);

			// Setting Dialog Message
			alertDialog1.setMessage(getString(R.string.scanner_permission_rationale));

			// Setting OK Button

			alertDialog1.setButton(AlertDialog.BUTTON_NEGATIVE,"NEXT", (dialog, which) -> {
				String[] permissions = new String[arrPerm.size()];
				permissions = arrPerm.toArray(permissions);
				ActivityCompat.requestPermissions(FeaturesActivity.this, permissions, 0);
				dialog.cancel();
			});

			// Showing Alert Message
			alertDialog1.show();
		}

		// Enable access to internet
		// ThreadPolicy to get permission to access internet
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// configure the app grid
		final GridView grid = findViewById(R.id.grid);
		grid.setAdapter(new AppAdapter(this));
		grid.setEmptyView(findViewById(android.R.id.empty));

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.help, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull final MenuItem item) {

		int itemId = item.getItemId();

		if (itemId == R.id.action_about) {
			final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.about_text, true);
			fragment.show(getSupportFragmentManager(), null);
		} else { //if (itemId == R.id.home) {
			this.finishAffinity();
//			onBackPressed();
		}
		return true;
	}

	private boolean ensureBLEExists() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
}
