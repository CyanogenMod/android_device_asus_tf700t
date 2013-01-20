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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;

public class DeviceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "DeviceBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            // Start/Stop LTO download service when connectivity changes
            boolean hasConnection =
                    !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

            Log.d(TAG, "Got connectivity change, has connection: " + hasConnection);
            Intent serviceIntent = new Intent(context, LtoDownloadService.class);

            if (hasConnection) {
                context.startService(serviceIntent);
            } else {
                context.stopService(serviceIntent);
            }

        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // Restore CPU mode
            try {
                String cpuMode = CpuUtils.getCpuMode();
                boolean cpuSetOnBoot =
                        prefs.getBoolean(CpuUtils.PREFERENCE_CPU_SET_ON_BOOT, false);
                Log.i(TAG,
                        String.format("Set CPU mode on boot %s.", String.valueOf(cpuSetOnBoot)));
                Log.i(TAG,
                        String.format("Current CPU mode %s.", String.valueOf(cpuMode)));
                if (cpuSetOnBoot) {
                    cpuMode =
                        prefs.getString(
                                CpuUtils.PREFERENCE_CPU_MODE, CpuUtils.DEFAULT_CPU_MODE);
                    CpuUtils.setCpuMode(cpuMode);

                    Log.i(TAG, String.format("Applied CPU mode on boot to value %s.", cpuMode));
                }
            } catch (Exception ex) {
                Log.e(TAG, "CPU set on boot failed", ex);
            }

            // Schedule LTO download
            try {
                long lastDownload = LtoDownloadUtils.getLastDownload(context);
                long next = LtoDownloadUtils.scheduleNextDownload(context, lastDownload);

                Log.i(TAG, String.format(
                            "Scheduled LTO download. Next time: %s.",
                            String.valueOf(new Date(next))));
            } catch (Exception ex) {
                Log.e(TAG, "Schedule of LTO data download failed", ex);
            }
        }

    }
}
