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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class LtoDownloadUtils {
    private static final String TAG = "LtoDownloadUtils";
    private static final long DOWNLOAD_INTERVAL_DEFAULT = 3600 * 24 * 3 * 1000; /* 3 days */

    static long getLastDownload(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong(LtoDownloadService.KEY_LAST_DOWNLOAD, 0);
    }

    static long getDownloadInterval(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String value = prefs.getString(LtoDownloadService.KEY_INTERVAL, null);
        if (value != null) {
            try {
                return Long.parseLong(value) * 1000;
            } catch (NumberFormatException e) {
                Log.w(TAG, "Found invalid interval " + value);
            }
        }
        return DOWNLOAD_INTERVAL_DEFAULT;
    }

    static long scheduleNextDownload(Context context, long lastDownload) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, LtoDownloadService.class);
        PendingIntent pi = PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        long nextLtoDownload = lastDownload + getDownloadInterval(context);
        am.set(AlarmManager.RTC, nextLtoDownload, pi);
        return nextLtoDownload;
    }
}
