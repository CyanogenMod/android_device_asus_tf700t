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

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cyanogenmod.asusdec.DockEmbeddedController;
import com.cyanogenmod.asusdec.KeyHandler;

import java.util.Date;

public class DeviceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "DeviceBroadcastReceiver";

    private static final int NOTIFICATION_ID = R.string.dock_kp_notifications;

    private final Handler mHandler;
    private static final Object mLock = new Object();
    private NotificationManager mNotificationManager;
    private int mNotificationTimeout;

    private Runnable mCancelKpNotifications = new Runnable() {
        @Override
        public void run() {
            synchronized (mLock) {
                if (mNotificationManager != null) {
                    mNotificationManager.cancel(NOTIFICATION_ID);
                }
            }
        }
    };

    public DeviceBroadcastReceiver() {
        super();
        mHandler = new Handler();
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        final String action = intent.getAction();
        if (KeyHandler.ACTION_DOCK_KEYPAD_KEY_PRESSED.equals(action)) {
            checkKeyPadKeyPressed(ctx, intent);

        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            checkConnectivity(ctx, intent);

        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            checkBootComplete(ctx, intent);

        } else if (Intent.ACTION_DOCK_EVENT.equals(action)) {
            int state = intent.getIntExtra(
                                Intent.EXTRA_DOCK_STATE, Intent.EXTRA_DOCK_STATE_UNDOCKED);
            // On undock unset ec_wakeup. Otherwise, restore the last setting status
            if (state == Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                setEcWakeMode(false);
            } else {
                restoreEcWakeMode(ctx);
            }
        }
    }

    private void checkKeyPadKeyPressed(Context ctx, Intent intent) {

        boolean displayNotifications = DockUtils.getKpNotifications(ctx);
        if (!displayNotifications) {
            return;
        }

        synchronized (mLock) {
            if (mNotificationManager == null) {
                mNotificationManager =
                        (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                Resources res = ctx.getResources();
                mNotificationTimeout = res.getInteger(R.integer.kp_notification_timeout);
            }
        }

        // Get extras
        int key = intent.getIntExtra(
                KeyHandler.EXTRA_ASUSDEC_KEY, KeyHandler.ASUSDEC_UNKNOWN);
        int status = intent.getIntExtra(
                KeyHandler.EXTRA_ASUSDEC_STATUS, KeyHandler.ASUSDEC_STATUS_OFF);
        String title = null;
        int icon = com.android.internal.R.drawable.ic_settings_language;

        // Choose the notification from key
        switch (key) {
            case KeyHandler.ASUSDEC_WIFI:
                title = ctx.getString(
                        (status == KeyHandler.ASUSDEC_STATUS_ON) ?
                            R.string.kp_wifi_on :
                            R.string.kp_wifi_off);
                break;

            case KeyHandler.ASUSDEC_BT:
                title = ctx.getString(
                        (status == KeyHandler.ASUSDEC_STATUS_ON) ?
                            R.string.kp_bt_on :
                            R.string.kp_bt_off);
                break;

            case KeyHandler.ASUSDEC_TOUCHPAD:
                title = ctx.getString(
                        (status == KeyHandler.ASUSDEC_STATUS_ON) ?
                            R.string.kp_tp_on :
                            R.string.kp_tp_off);
                break;

            case KeyHandler.ASUSDEC_BRIGHTNESS:
                if (status == KeyHandler.ASUSDEC_STATUS_ON) {
                    title = ctx.getString(R.string.kp_brightness_auto);
                } else {
                    int value = intent.getIntExtra(KeyHandler.EXTRA_ASUSDEC_VALUE, 0);
                    title = ctx.getString(R.string.kp_brightness_manual, value);
                }
                break;

            case KeyHandler.ASUSDEC_VOLUME:
                int value = intent.getIntExtra(KeyHandler.EXTRA_ASUSDEC_VALUE, 0);
                title = ctx.getString(R.string.kp_volume, String.valueOf(value));
                break;

            case KeyHandler.ASUSDEC_VOLUME_MUTE:
                title = ctx.getString(
                        (status == KeyHandler.ASUSDEC_STATUS_ON) ?
                            R.string.kp_volume_mute_on :
                            R.string.kp_volume_mute_off);
                break;

            case KeyHandler.ASUSDEC_MEDIA:
                int res = R.string.kp_media_play_pause;
                if (status == KeyHandler.ASUSDEC_MEDIA_PREVIOUS) {
                    res = R.string.kp_media_previous;
                } else if (status == KeyHandler.ASUSDEC_MEDIA_NEXT) {
                    res = R.string.kp_media_next;
                }
                title = ctx.getString(res);
                break;

            case KeyHandler.ASUSDEC_SCREENSHOT:
                title = ctx.getString(R.string.kp_screenshot);
                break;

            case KeyHandler.ASUSDEC_EXPLORER:
                title = ctx.getString(R.string.kp_explorer);
                break;

            case KeyHandler.ASUSDEC_SETTINGS:
                title = ctx.getString(R.string.kp_settings);
                break;

            case KeyHandler.ASUSDEC_CAPS_LOCK:
                title = ctx.getString(
                        (status == KeyHandler.ASUSDEC_STATUS_ON) ?
                            R.string.kp_caps_lock_on :
                            R.string.kp_caps_lock_off);
                break;

            default:
                break;
        }

        // Display the notification?
        if (title != null) {
            displayKeyPadNotification(ctx, title, icon);
        }
    }

    private void checkConnectivity(Context ctx, Intent intent) {
        // Start/Stop LTO download service when connectivity changes
        boolean hasConnection =
                !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

        Log.d(TAG, "Got connectivity change, has connection: " + hasConnection);
        Intent serviceIntent = new Intent(ctx, LtoDownloadService.class);

        if (hasConnection) {
            ctx.startService(serviceIntent);
        } else {
            ctx.stopService(serviceIntent);
        }
    }

    private void checkBootComplete(Context ctx, Intent intent) {
        // Restore CPU mode
        restoreCpuMode(ctx);

        // Restore Function keys
        restoreFunctionKeys(ctx);

        // Restore Nvidia Smartdimmer mode
        restoreSmartdimmerMode(ctx);

        // Restore dock EcWakeUp mode
        restoreEcWakeMode(ctx);

        // Schedule LTO download
        scheduleLtoDownload(ctx);
    }

    private void restoreCpuMode(Context ctx) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
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
    }

    private void restoreFunctionKeys(Context ctx) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            long meta =
                    DockUtils.translateFunctionKeysToMetaKeys(
                        DockUtils.getKpFunctionKeys(ctx));
            DockUtils.setMetaFunctionKeys(meta);
            Log.i(TAG,
                    String.format("Set Function keys on boot %s.", String.valueOf(meta)));
        } catch (Exception ex) {
            Log.e(TAG, "Function keys set on boot failed", ex);
        }
    }

    private void restoreSmartdimmerMode(Context ctx) {
        try {
            boolean smartdimmer = DisplayUtils.getSmartdimmer(ctx);
            Log.i(TAG, "Restore Smartdimmer: " + smartdimmer);
            if (!DisplayUtils.writeSmartdimmerStatus(smartdimmer)) {
                Log.w(TAG, "Restore Smartdimmer failed.");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Restore Smartdimmer failed", ex);
        }
    }

    private void restoreEcWakeMode(Context ctx) {
        try {
            DockEmbeddedController dockEc = new DockEmbeddedController();
            boolean ecWakeUp = DockUtils.getEcWakeUp(ctx);
            Log.i(TAG, "Restore EcWakeUp: " + ecWakeUp);
            if (!dockEc.setECWakeUp(ecWakeUp)) {
                Log.w(TAG, "Restore EcWakeUp failed.");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Restore EcWakeUp failed", ex);
        }
    }

    private void setEcWakeMode(boolean on) {
        try {
            DockEmbeddedController dockEc = new DockEmbeddedController();
            Log.i(TAG, "Set EcWakeUp: " + on);
            if (!dockEc.setECWakeUp(on)) {
                Log.w(TAG, "Set EcWakeUp failed.");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Set EcWakeUp failed", ex);
        }
    }

    private void scheduleLtoDownload(Context ctx) {
        try {
            long lastDownload = LtoDownloadUtils.getLastDownload(ctx);
            long next = LtoDownloadUtils.scheduleNextDownload(ctx, lastDownload);

            Log.i(TAG, String.format(
                        "Scheduled LTO download. Next time: %s.",
                        String.valueOf(new Date(next))));
        } catch (Exception ex) {
            Log.e(TAG, "Schedule of LTO data download failed", ex);
        }
    }

    private void displayKeyPadNotification(Context ctx, String title, int icon) {
        // Cancel any running notification callback
        synchronized (mLock) {
            mHandler.removeCallbacks(mCancelKpNotifications);
        }

        Notification notification = new Notification.Builder(ctx)
                .setContentTitle(title)
                .setTicker(title)
                .setSmallIcon(icon)
                .setDefaults(0) // be quiet
                .setSound(null)
                .setVibrate(null)
                .setOngoing(true)
                .setWhen(0)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
        mNotificationManager.notifyAsUser(null, NOTIFICATION_ID, notification, UserHandle.ALL);

        // Post a delayed cancellation of the notification
        mHandler.postDelayed(mCancelKpNotifications, mNotificationTimeout);
    }
}
