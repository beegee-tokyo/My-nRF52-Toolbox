package tk.giesecke.my_nrf52_tb;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import static tk.giesecke.my_nrf52_tb.FeaturesActivity.buttonNames;
import static tk.giesecke.my_nrf52_tb.FeaturesActivity.buttonValues;
import static tk.giesecke.my_nrf52_tb.FeaturesActivity.reqUUID;
import static tk.giesecke.my_nrf52_tb.uart.UARTService.SETTINGS_CLOSE;

public class SettingsActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_activity);
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.settings, new SettingsFragment())
				.commit();
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void onDestroy() {
		Log.d("PREFS", "Prefs closing");
		final Intent broadcast = new Intent(SETTINGS_CLOSE);
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			NavUtils.navigateUpFromSameTask(this);
		}
		return super.onOptionsItemSelected(item);
	}

	public static class SettingsFragment extends PreferenceFragmentCompat {
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.root_preferences, rootKey);
			CheckBoxPreference uuidEnabled = findPreference("uuid_filter");
			assert uuidEnabled != null;
			uuidEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
				FeaturesActivity.reqUUIDena = !((CheckBoxPreference) preference).isChecked();
				return true;
			});

			EditTextPreference uuidFilter = findPreference("uuid_filter_value");
			assert uuidFilter != null;
			uuidFilter.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				String newUuidFilter = newValue.toString();
				if (newUuidFilter.isEmpty()) {
					Toast.makeText(getContext(), R.string.settings_invalidUUID, Toast.LENGTH_SHORT).show();
					return false;
				}
				String tempUUID = newValue.toString();
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
						Toast.makeText(getContext(), R.string.settings_invalidUUID, Toast.LENGTH_SHORT).show();
					}
				}
				return true;
			});

			CheckBoxPreference nameFilterEnabled = findPreference("name_filter");
			assert nameFilterEnabled != null;
			nameFilterEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
				if (!((CheckBoxPreference) preference).isChecked()) {
					FeaturesActivity.devicePrefix = "";
				}
				else {
					EditTextPreference devicePrefix = findPreference("name_filter_value");
					assert devicePrefix != null;
					FeaturesActivity.devicePrefix = devicePrefix.getText();
				}
				return true;
			});

			EditTextPreference nameFilter = findPreference("name_filter_value");
			assert nameFilter != null;
			nameFilter.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				CheckBoxPreference nameFilterEnabled1 = findPreference("name_filter");
				assert nameFilterEnabled1 != null;
				if (nameFilterEnabled1.isChecked()) {
					FeaturesActivity.devicePrefix = newValue.toString();
				} else {
					FeaturesActivity.devicePrefix = "";
				}
				return true;
			});
			EditTextPreference bt0Name = findPreference("bt0_name_value");
			assert bt0Name != null;
			bt0Name.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonNames[0] = newValue.toString();
				Button changeBt = Objects.requireNonNull(getActivity()).findViewById(R.id.bt0);
				if (changeBt != null) {
					changeBt.setText(buttonNames[0]);
				}
				return true;
			});
			EditTextPreference bt1Name = findPreference("bt1_name_value");
			assert bt1Name != null;
			bt1Name.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonNames[1] = newValue.toString();
				Button changeBt = Objects.requireNonNull(getActivity()).findViewById(R.id.bt1);
				if (changeBt != null) {
					changeBt.setText(buttonNames[1]);
				}
				return true;
			});
			EditTextPreference bt2Name = findPreference("bt2_name_value");
			assert bt2Name != null;
			bt2Name.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonNames[2] = newValue.toString();
				Button changeBt = Objects.requireNonNull(getActivity()).findViewById(R.id.bt2);
				if (changeBt != null) {
					changeBt.setText(buttonNames[2]);
				}
				return true;
			});
			EditTextPreference bt3Name = findPreference("bt3_name_value");
			assert bt3Name != null;
			bt3Name.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonNames[3] = newValue.toString();
				Button changeBt = Objects.requireNonNull(getActivity()).findViewById(R.id.bt3);
				if (changeBt != null) {
					changeBt.setText(buttonNames[3]);
				}
				return true;
			});
			EditTextPreference bt4Name = findPreference("bt4_name_value");
			assert bt4Name != null;
			bt4Name.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonNames[4] = newValue.toString();
				Button changeBt = Objects.requireNonNull(getActivity()).findViewById(R.id.bt4);
				if (changeBt != null) {
					changeBt.setText(buttonNames[4]);
				}
				return true;
			});
			EditTextPreference bt5Name = findPreference("bt5_name_value");
			assert bt5Name != null;
			bt5Name.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonNames[5] = newValue.toString();
				Button changeBt = Objects.requireNonNull(getActivity()).findViewById(R.id.bt5);
				if (changeBt != null) {
					changeBt.setText(buttonNames[5]);
				}
				return true;
			});
			EditTextPreference bt6Name = findPreference("bt6_name_value");
			assert bt6Name != null;
			bt6Name.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonNames[6] = newValue.toString();
				Button changeBt = Objects.requireNonNull(getActivity()).findViewById(R.id.bt6);
				if (changeBt != null) {
					changeBt.setText(buttonNames[6]);
				}
				return true;
			});
			EditTextPreference bt7Name = findPreference("bt7_name_value");
			assert bt7Name != null;
			bt7Name.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonNames[7] = newValue.toString();
				Button changeBt = Objects.requireNonNull(getActivity()).findViewById(R.id.bt7);
				if (changeBt != null) {
					changeBt.setText(buttonNames[7]);
				}
				return true;
			});
			EditTextPreference bt8Name = findPreference("bt8_name_value");
			assert bt8Name != null;
			bt8Name.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonNames[8] = newValue.toString();
				Button changeBt = Objects.requireNonNull(getActivity()).findViewById(R.id.bt8);
				if (changeBt != null) {
					changeBt.setText(buttonNames[8]);
				}
				return true;
			});
			EditTextPreference bt9Name = findPreference("bt9_name_value");
			assert bt9Name != null;
			bt9Name.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonNames[9] = newValue.toString();
				Button changeBt = Objects.requireNonNull(getActivity()).findViewById(R.id.bt9);
				if (changeBt != null) {
					changeBt.setText(buttonNames[9]);
				}
				return true;
			});
			EditTextPreference bt0Val = findPreference("bt0_value");
			assert bt0Val != null;
			bt0Val.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonValues[0] = newValue.toString();
				return true;
			});
			EditTextPreference bt1Val = findPreference("bt1_value");
			assert bt1Val != null;
			bt1Val.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonValues[1] = newValue.toString();
				return true;
			});
			EditTextPreference bt2Val = findPreference("bt2_value");
			assert bt2Val != null;
			bt2Val.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonValues[2] = newValue.toString();
				return true;
			});
			EditTextPreference bt3Val = findPreference("bt3_value");
			assert bt3Val != null;
			bt3Val.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonValues[3] = newValue.toString();
				return true;
			});
			EditTextPreference bt4Val = findPreference("bt4_value");
			assert bt4Val != null;
			bt4Val.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonValues[4] = newValue.toString();
				return true;
			});
			EditTextPreference bt5Val = findPreference("bt5_value");
			assert bt5Val != null;
			bt5Val.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonValues[5] = newValue.toString();
				return true;
			});
			EditTextPreference bt6Val = findPreference("bt6_value");
			assert bt6Val != null;
			bt6Val.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonValues[6] = newValue.toString();
				return true;
			});
			EditTextPreference bt7Val = findPreference("bt7_value");
			assert bt7Val != null;
			bt7Val.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonValues[7] = newValue.toString();
				return true;
			});
			EditTextPreference bt8Val = findPreference("bt8_value");
			assert bt8Val != null;
			bt8Val.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonValues[8] = newValue.toString();
				return true;
			});
			EditTextPreference bt9Val = findPreference("bt9_value");
			assert bt9Val != null;
			bt9Val.setOnPreferenceChangeListener((preference, newValue) -> {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(newValue.toString());
				buttonValues[9] = newValue.toString();
				return true;
			});
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