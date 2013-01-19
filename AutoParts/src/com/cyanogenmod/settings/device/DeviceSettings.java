/*
 * Copyright (C) 2013 The CyanogenMod Project
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

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.MenuItem;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class DeviceSettings extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // This activity is always called from another activity, so we
        // assume that HOME_UP button should be always displayed
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_header, target);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       switch (item.getItemId()) {
          case android.R.id.home:
              finish();
              return true;
          default:
             return super.onOptionsItemSelected(item);
       }
    }

    public static class CpuSettingsFragment
        extends PreferenceFragment implements OnPreferenceChangeListener {

        private ListPreference mCpuMode;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences_cpu);

            String currCpuMode = CpuUtils.CPU_SETTING_PERFORMANCE;
            if (CpuUtils.getCpuMode() != null) {
                currCpuMode = CpuUtils.getCpuMode();
            }

            mCpuMode = (ListPreference)findPreference(CpuUtils.PREFERENCE_CPU_MODE);
            mCpuMode.setValueIndex(getCpuModeIndex(currCpuMode));
            mCpuMode.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            if (key.compareTo(CpuUtils.PREFERENCE_CPU_MODE) == 0) {
                final String newCpuMode = (String)newValue;
                CpuUtils.setCpuMode(newCpuMode);
            }
            return true;
        }

        private int getCpuModeIndex(String mode) {
            if (mode.equals(CpuUtils.CPU_SETTING_POWER_SAVE)) {
                return Integer.parseInt(CpuUtils.CPU_SETTING_POWER_SAVE);
            }
            if (mode.equals(CpuUtils.CPU_SETTING_PERFORMANCE)) {
                return Integer.parseInt(CpuUtils.CPU_SETTING_PERFORMANCE);
            }
            return Integer.parseInt(CpuUtils.CPU_SETTING_BALANCED);
        }
    }

    public static class GpsSettingsFragment
        extends PreferenceFragment implements OnPreferenceChangeListener,
        OnPreferenceClickListener, OnSharedPreferenceChangeListener {

        private BroadcastReceiver mLtoStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(
                        LtoDownloadService.EXTRA_STATE, LtoDownloadService.STATE_IDLE);

                mLtoDownloadNowPref.setEnabled(state == LtoDownloadService.STATE_IDLE);
                if (state == LtoDownloadService.STATE_IDLE) {
                    boolean success = intent.getBooleanExtra(
                                            LtoDownloadService.EXTRA_SUCCESS, true);
                    long timestamp = intent.getLongExtra(LtoDownloadService.EXTRA_TIMESTAMP, 0);
                    updateLtoDownloadDateSummary(
                            success,
                            timestamp == 0 ? null : new Date(timestamp));
                } else {
                    int progress = intent.getIntExtra(LtoDownloadService.EXTRA_PROGRESS, 0);
                    updateLtoDownloadProgressSummary(progress);
                }
            }
        };

        private BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean hasConnection =
                        !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (mLtoDownloadNowPref != null) {
                    mLtoDownloadNowPref.setEnabled(hasConnection);
                }
            }
        };

        private static final String PREFERENCE_LTO_ENABLED = LtoDownloadService.KEY_ENABLED;
        private static final String PREFERENCE_LTO_INTERVAL = LtoDownloadService.KEY_INTERVAL;
        private static final String PREFERENCE_LTO_WIFI_ONLY = LtoDownloadService.KEY_WIFI_ONLY;
        private static final String PREFERENCE_LTO_FILE_TYPE = LtoDownloadService.KEY_FILE_TYPE;
        private static final String PREFERENCE_LTO_DOWNLOAD_NOW = "lto_download_now";

        private CheckBoxPreference mLtoDownloadEnabledPref;
        private ListPreference mLtoDownloadIntervalPref;
        private ListPreference mLtoDownloadFilePref;
        private CheckBoxPreference mLtoDownloadWifiOnlyPref;
        private Preference mLtoDownloadNowPref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences_gps);

            mLtoDownloadEnabledPref = (CheckBoxPreference)findPreference(PREFERENCE_LTO_ENABLED);
            mLtoDownloadEnabledPref.setOnPreferenceChangeListener(this);

            mLtoDownloadIntervalPref = (ListPreference)findPreference(PREFERENCE_LTO_INTERVAL);
            updateLtoIntervalSummary();
            mLtoDownloadIntervalPref.setOnPreferenceChangeListener(this);

            mLtoDownloadWifiOnlyPref = (CheckBoxPreference)findPreference(PREFERENCE_LTO_WIFI_ONLY);
            mLtoDownloadWifiOnlyPref.setOnPreferenceChangeListener(this);

            mLtoDownloadFilePref = (ListPreference)findPreference(PREFERENCE_LTO_FILE_TYPE);
            updateLtoFileSummary();
            mLtoDownloadFilePref.setOnPreferenceChangeListener(this);

            mLtoDownloadNowPref = findPreference(PREFERENCE_LTO_DOWNLOAD_NOW);
            updateLtoDownloadDateSummary(true, null);
            mLtoDownloadNowPref.setOnPreferenceClickListener(this);

            ConnectivityManager cm =
                    (ConnectivityManager)getActivity().getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            mLtoDownloadNowPref.setEnabled(info != null && !info.isConnected());
        }

        @Override
        public void onResume() {
            super.onResume();

            ConnectivityManager cm =
                    (ConnectivityManager)getActivity().getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            mLtoDownloadNowPref.setEnabled(info != null && !info.isConnected());

            updateLtoIntervalSummary();
            updateLtoFileSummary();
            updateLtoDownloadDateSummary(true, null);

            IntentFilter filterLto = new IntentFilter(LtoDownloadService.ACTION_STATE_CHANGE);
            getActivity().registerReceiver(mLtoStateReceiver, filterLto);
            IntentFilter filterConn = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            getActivity().registerReceiver(mConnectivityReceiver, filterConn);

            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            prefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getActivity().unregisterReceiver(mLtoStateReceiver);
            getActivity().unregisterReceiver(mConnectivityReceiver);

            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            if (key.compareTo(PREFERENCE_LTO_ENABLED) == 0 ||
                key.compareTo(PREFERENCE_LTO_INTERVAL) == 0 ||
                key.compareTo(PREFERENCE_LTO_WIFI_ONLY) == 0 ||
                key.compareTo(PREFERENCE_LTO_FILE_TYPE) == 0) {

                invokeLtoDownloadService(false);
            }
            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String key = preference.getKey();
            if (key.compareTo(PREFERENCE_LTO_DOWNLOAD_NOW) == 0) {
                invokeLtoDownloadService(true);
            }
            return false;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
            if (key.compareTo(PREFERENCE_LTO_INTERVAL) == 0) {
                updateLtoIntervalSummary();
            }
            if (key.compareTo(PREFERENCE_LTO_FILE_TYPE) == 0) {
                updateLtoFileSummary();
            }
        }

        private void invokeLtoDownloadService(boolean forceDownload) {
            Intent intent = new Intent(getActivity(), LtoDownloadService.class);
            intent.putExtra(LtoDownloadService.EXTRA_FORCE_DOWNLOAD, forceDownload);
            getActivity().startService(intent);
        }

        private void updateLtoIntervalSummary() {
            mLtoDownloadIntervalPref.setSummary(mLtoDownloadIntervalPref.getEntry());
        }

        private void updateLtoFileSummary() {
            mLtoDownloadFilePref.setSummary(mLtoDownloadFilePref.getEntry());
        }

        private void updateLtoDownloadProgressSummary(int progress) {
            mLtoDownloadNowPref.setSummary(
                    getResources().getString(R.string.lto_downloading_data, progress));
        }

        private void updateLtoDownloadDateSummary(boolean success, Date timestamp) {
            Resources res = getResources();
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();

            if (timestamp == null) {
                long lastDownload = prefs.getLong(LtoDownloadService.KEY_LAST_DOWNLOAD, 0);
                if (lastDownload != 0) {
                    timestamp = new Date(lastDownload);
                }
            }

            String summary = "";
            if (timestamp != null) {
                String lastDownloadTime = DateFormat.getDateTimeInstance().format(timestamp);
                int resId = success ?
                                    R.string.lto_last_download_date :
                                    R.string.lto_last_download_date_failure;
                summary = res.getString(resId, lastDownloadTime);
            } else {
                summary = res.getString(R.string.lto_last_download_date_never);
            }

            mLtoDownloadNowPref.setSummary(summary);
        }
    }

}
