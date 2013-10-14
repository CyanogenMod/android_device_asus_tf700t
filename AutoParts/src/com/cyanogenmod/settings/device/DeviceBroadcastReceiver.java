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
import android.content.res.Resources;
import android.os.Handler;
import android.os.UserHandle;

import com.cyanogenmod.asusdec.KeyHandler;

public class DeviceBroadcastReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = R.string.app_name;

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
        }
    }

    private void checkKeyPadKeyPressed(Context ctx, Intent intent) {

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
            displayNotification(ctx, title, icon);
        }
    }

    private void displayNotification(Context ctx, String title, int icon) {
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
