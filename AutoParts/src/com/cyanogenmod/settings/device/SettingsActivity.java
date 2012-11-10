package com.cyanogenmod.settings.device;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class SettingsActivity extends PreferenceActivity
        implements OnPreferenceChangeListener, OnSharedPreferenceChangeListener {
    private static final String TAG = "DeviceSettings";

    private PackageManager mPm;

    private CheckBoxPreference mLtoDownloadEnabledPref;
    private ListPreference mLtoDownloadIntervalPref;
    private ListPreference mLtoDownloadFilePref;
    private CheckBoxPreference mLtoDownloadWifiOnlyPref;
    private Preference mLtoDownloadNowPref;

    private BroadcastReceiver mLtoStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(LtoDownloadService.EXTRA_STATE, LtoDownloadService.STATE_IDLE);

            mLtoDownloadNowPref.setEnabled(state == LtoDownloadService.STATE_IDLE);
            if (state == LtoDownloadService.STATE_IDLE) {
                boolean success = intent.getBooleanExtra(LtoDownloadService.EXTRA_SUCCESS, true);
                long timestamp = intent.getLongExtra(LtoDownloadService.EXTRA_TIMESTAMP, 0);
                updateLtoDownloadDateSummary(success, timestamp == 0 ? null : new Date(timestamp));
            } else {
                int progress = intent.getIntExtra(LtoDownloadService.EXTRA_PROGRESS, 0);
                updateLtoDownloadProgressSummary(progress);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        mPm = getPackageManager();

        PreferenceScreen prefs = getPreferenceScreen();

        mLtoDownloadEnabledPref = (CheckBoxPreference) prefs.findPreference("lto_download_enabled");
        mLtoDownloadEnabledPref.setOnPreferenceChangeListener(this);
        mLtoDownloadIntervalPref = (ListPreference) prefs.findPreference("lto_download_interval");
        mLtoDownloadIntervalPref.setOnPreferenceChangeListener(this);
        mLtoDownloadFilePref = (ListPreference) prefs.findPreference("lto_download_file");
        mLtoDownloadFilePref.setOnPreferenceChangeListener(this);
        mLtoDownloadWifiOnlyPref = (CheckBoxPreference) prefs.findPreference("lto_download_wifi_only");
        mLtoDownloadWifiOnlyPref.setOnPreferenceChangeListener(this);
        mLtoDownloadNowPref = prefs.findPreference("lto_download_now");
    }

    @Override
    public void onResume() {
        super.onResume();

        updateLtoDownloadDateSummary(true, null);
        updateLtoIntervalSummary();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        registerReceiver(mLtoStateReceiver, new IntentFilter(LtoDownloadService.ACTION_STATE_CHANGE));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mLtoStateReceiver);
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference == mLtoDownloadNowPref) {
            invokeLtoDownloadService(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLtoDownloadEnabledPref
                || preference == mLtoDownloadIntervalPref
                || preference == mLtoDownloadFilePref
                || preference == mLtoDownloadWifiOnlyPref) {
            invokeLtoDownloadService(false);
        }

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        if (key.equals(mLtoDownloadIntervalPref.getKey())) {
            updateLtoIntervalSummary();
        }
    }

    private void updateLtoIntervalSummary() {
        CharSequence value = mLtoDownloadIntervalPref.getEntry();
        mLtoDownloadIntervalPref.setSummary(
                getResources().getString(R.string.lto_download_interval_summary, value));
    }

    private void updateLtoDownloadProgressSummary(int progress) {
        mLtoDownloadNowPref.setSummary(
                getResources().getString(R.string.lto_downloading_data, progress));
    }

    private void updateLtoDownloadDateSummary(boolean success, Date timestamp) {
        Resources res = getResources();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String lastDownloadString;
        int resId = R.string.lto_last_download_date;

        if (timestamp == null) {
            long lastDownload = prefs.getLong(LtoDownloadService.KEY_LAST_DOWNLOAD, 0);
            if (lastDownload != 0) {
                timestamp = new Date(lastDownload);
            }
        }

        if (timestamp != null) {
            lastDownloadString = DateFormat.getDateTimeInstance().format(timestamp);
            if (!success) {
                resId = R.string.lto_last_download_date_failure;
            }
        } else {
            lastDownloadString = res.getString(R.string.never);
        }

        mLtoDownloadNowPref.setSummary(res.getString(resId, lastDownloadString));
    }

    private void invokeLtoDownloadService(boolean forceDownload) {
        Intent intent = new Intent(this, LtoDownloadService.class);
        intent.putExtra(LtoDownloadService.EXTRA_FORCE_DOWNLOAD, forceDownload);
        startService(intent);
    }
}
