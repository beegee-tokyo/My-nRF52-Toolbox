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

package tk.giesecke.my_nrf52_tb.setup_lora.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;

import tk.giesecke.my_nrf52_tb.R;
import tk.giesecke.my_nrf52_tb.setup_lora.LoRaActivity;

import static tk.giesecke.my_nrf52_tb.uart.UARTService.SETTINGS_CLOSE;

public class SettingsActivity extends AppCompatActivity {

	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		final Toolbar toolbar  = findViewById(R.id.toolbar_actionbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Display the fragment as the main content.
		getSupportFragmentManager().beginTransaction().replace(R.id.content, new SettingsFragment()).commit();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.settings_generic);
//		getSupportFragmentManager()
//				.beginTransaction()
//				.replace(R.id.settings, new LoRaSettingsFragment())
//				.commit();
//		ActionBar actionBar = getSupportActionBar();
//		if (actionBar != null) {
//			actionBar.setDisplayHomeAsUpEnabled(true);
//		}
//	}

	@Override
	protected void onDestroy() {
		Log.d("PREFS", "Prefs closing");
		final Intent broadcast = new Intent(SETTINGS_CLOSE);
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
		super.onDestroy();
	}

//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		int id = item.getItemId();
//		if (id == android.R.id.home) {
//			NavUtils.navigateUpFromSameTask(this);
//		}
//		return super.onOptionsItemSelected(item);
//	}

	public static class LoRaSettingsFragment extends PreferenceFragmentCompat {
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.settings_lora, rootKey);

			CheckBoxPreference loraMode = findPreference("lora_mode");
			if (loraMode.isChecked()) {
				Preference changePref = findPreference("p2p_prefs");
				changePref.setEnabled(false);
				changePref.setVisible(false);
				changePref = findPreference("lpwan_prefs");
				changePref.setEnabled(true);
				changePref.setVisible(true);
			} else {
				Preference changePref = findPreference("p2p_prefs");
				changePref.setEnabled(true);
				changePref.setVisible(true);
				changePref = findPreference("lpwan_prefs");
				changePref.setEnabled(false);
				changePref.setVisible(false);
			}
			if (loraMode != null)
			{
				loraMode.setOnPreferenceChangeListener((preference, newValue) -> {
					LoRaActivity.loraWanEna = !((CheckBoxPreference) preference).isChecked();
					if (!((CheckBoxPreference) preference).isChecked()) {
						Preference changePref = findPreference("p2p_prefs");
						changePref.setEnabled(false);
						changePref.setVisible(false);
						changePref = findPreference("lpwan_prefs");
						changePref.setEnabled(true);
						changePref.setVisible(true);
					} else {
						Preference changePref = findPreference("p2p_prefs");
						changePref.setEnabled(true);
						changePref.setVisible(true);
						changePref = findPreference("lpwan_prefs");
						changePref.setEnabled(false);
						changePref.setVisible(false);
					}
					return true;
				});
			}
			EditTextPreference devEUI = findPreference("dev_eui");
			if (devEUI != null) {
				devEUI.setOnPreferenceChangeListener((preference, newValue) -> {
					EditTextPreference listPreference = (EditTextPreference) preference;
					listPreference.setSummary(newValue.toString());
					LoRaActivity.nodeDeviceEUI = newValue.toString();
					EditText changeText = getActivity().findViewById(R.id.lora_dev_eui);
					changeText.setText(LoRaActivity.nodeDeviceEUI);
					return true;
				});
			}
			EditTextPreference appEUI = findPreference("app_eui");
			if (appEUI != null) {
				appEUI.setOnPreferenceChangeListener((preference, newValue) -> {
					EditTextPreference listPreference = (EditTextPreference) preference;
					listPreference.setSummary(newValue.toString());
					LoRaActivity.nodeAppEUI = newValue.toString();
					EditText changeText = getActivity().findViewById(R.id.lora_app_eui);
					changeText.setText(LoRaActivity.nodeAppEUI);
					return true;
				});
			}
			EditTextPreference appKey = findPreference("app_key");
			if (appKey != null) {
				appKey.setOnPreferenceChangeListener((preference, newValue) -> {
					EditTextPreference listPreference = (EditTextPreference) preference;
					listPreference.setSummary(newValue.toString());
					LoRaActivity.nodeAppKey = newValue.toString();
					EditText changeText = getActivity().findViewById(R.id.lora_app_key);
					changeText.setText(LoRaActivity.nodeAppKey);
					return true;
				});
			}
			EditTextPreference devAdr = findPreference("dev_adr");
			if (devAdr != null) {
				devAdr.setOnPreferenceChangeListener((preference, newValue) -> {
					EditTextPreference listPreference = (EditTextPreference) preference;
					listPreference.setSummary(newValue.toString());
					LoRaActivity.nodeDeviceAddr = newValue.toString();
					EditText changeText = getActivity().findViewById(R.id.lora_dev_addr);
					changeText.setText(LoRaActivity.nodeDeviceAddr);
					return true;
				});
			}
		}

		@Override
		public void onResume() {
			super.onResume();
			for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
				Preference preference = getPreferenceScreen().getPreference(i);
				if (preference instanceof PreferenceGroup) {
					PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
					for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
						Preference singlePref = preferenceGroup.getPreference(j);
						updatePreference(singlePref);
					}
				} else {
					updatePreference(preference);
				}
			}
		}

		private void updatePreference(Preference preference) {
			if (preference == null) return;
			if (preference instanceof EditTextPreference) {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(listPreference.getText());
			}
		}
	}
}