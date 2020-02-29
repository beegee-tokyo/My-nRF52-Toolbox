package tk.giesecke.my_nrf52_tb;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

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
			EditTextPreference uuidFilter = findPreference("uuid_filter_value");
			uuidFilter.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					EditTextPreference listPreference = (EditTextPreference) preference;
					listPreference.setSummary(newValue.toString());
					return true;
				}
			});
			EditTextPreference nameFilter = findPreference("name_filter_value");
			nameFilter.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					EditTextPreference listPreference = (EditTextPreference) preference;
					listPreference.setSummary(newValue.toString());
					return true;
				}
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
						updatePreference(singlePref, singlePref.getKey());
					}
				} else {
					updatePreference(preference, preference.getKey());
				}
			}
		}

		private void updatePreference(Preference preference, String key) {
			if (preference == null) return;
			if (preference instanceof EditTextPreference) {
				EditTextPreference listPreference = (EditTextPreference) preference;
				listPreference.setSummary(listPreference.getText());
				return;
			}
//			SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();
//			preference.setSummary(sharedPrefs.getString(key, "Default"));
		}
	}
}