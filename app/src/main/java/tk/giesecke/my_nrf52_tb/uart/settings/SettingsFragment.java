package tk.giesecke.my_nrf52_tb.uart.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import tk.giesecke.my_nrf52_tb.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    public static final String SETTINGS_DATA = "settings_template_data"; // TODO values matching those in settings_template.xml file in /res/xml
    public static final int SETTINGS_VARIANT_A = 0;
    public static final int SETTINGS_VARIANT_B = 1;
    public static final int SETTINGS_VARIANT_DEFAULT = SETTINGS_VARIANT_A;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.settings_uart);
    }
}