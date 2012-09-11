/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.settings.device;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.os.SystemProperties;

public class DeviceSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {

    private static final String PREFERENCE_CPU_MODE = "cpu_settings";
    private static final String CPU_PROPERTY = "sys.cpu.mode";

    private ListPreference mCpuMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        String mCurrCpuMode = "1";

        if (SystemProperties.get(CPU_PROPERTY) != null)
            mCurrCpuMode = SystemProperties.get(CPU_PROPERTY);

        mCpuMode = (ListPreference) getPreferenceScreen().findPreference(
                PREFERENCE_CPU_MODE);

        mCpuMode.setValueIndex(getCpuModeOffset(mCurrCpuMode));
        mCpuMode.setOnPreferenceChangeListener(this);
    }

    private int getCpuModeOffset(String mode) {
        if (mode.equals("0")) {
            return 0;
        } else if (mode.equals("2")) {
            return 2;
        } else {
            return 1;
        }
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference.equals(mCpuMode)) {
            final String newCpuMode = (String) value;
            SystemProperties.set(CPU_PROPERTY, newCpuMode);
        }

        return true;
    }

}
