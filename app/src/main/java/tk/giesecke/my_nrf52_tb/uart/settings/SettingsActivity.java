package tk.giesecke.my_nrf52_tb.uart.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.appcompat.widget.Toolbar;

import tk.giesecke.my_nrf52_tb.R;

import static tk.giesecke.my_nrf52_tb.uart.UARTActivity.buttonNames;
import static tk.giesecke.my_nrf52_tb.uart.UARTActivity.buttonValues;
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
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.settings_generic);
//        getSupportFragmentManager()
//                .beginTransaction()
//                .replace(R.id.settings, new SettingsFragment())
//                .commit();
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true);
//        }
//    }

    @Override
    protected void onDestroy() {
        Log.d("PREFS", "Prefs closing");
        final Intent broadcast = new Intent(SETTINGS_CLOSE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
        super.onDestroy();
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == android.R.id.home) {
//            NavUtils.navigateUpFromSameTask(this);
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.settings_uart, rootKey);

            EditTextPreference bt0Name = findPreference("bt0_name_value");
            assert bt0Name != null;
            bt0Name.setOnPreferenceChangeListener((preference, newValue) -> {
                EditTextPreference listPreference = (EditTextPreference) preference;
                listPreference.setSummary(newValue.toString());
                buttonNames[0] = newValue.toString();
                Button changeBt = getActivity().findViewById(R.id.bt0);
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
                Button changeBt = getActivity().findViewById(R.id.bt1);
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
                Button changeBt = getActivity().findViewById(R.id.bt2);
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
                Button changeBt = getActivity().findViewById(R.id.bt3);
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
                Button changeBt = getActivity().findViewById(R.id.bt4);
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
                Button changeBt = getActivity().findViewById(R.id.bt5);
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
                Button changeBt = getActivity().findViewById(R.id.bt6);
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
                Button changeBt = getActivity().findViewById(R.id.bt7);
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
                Button changeBt = getActivity().findViewById(R.id.bt8);
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
                Button changeBt = getActivity().findViewById(R.id.bt9);
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