package com.mc.appguide;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

@SuppressWarnings("deprecation")
public class SettingsFragment extends PreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().setTitle("Settings");
        addPreferencesFromResource(R.xml.settings_preferences);
    }
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        return false;
    }
}