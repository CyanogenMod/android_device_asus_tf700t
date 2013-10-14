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

package com.cyanogenmod.asusdec;

import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.util.Slog;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.android.internal.os.DeviceKeyHandler;

public final class KeyHandler implements DeviceKeyHandler {
    private static final String TAG = "AsusdecKeyHandler";

    private static final boolean DEBUG = false;

    private static final int MINIMUM_BACKLIGHT = android.os.PowerManager.BRIGHTNESS_OFF + 1;
    private static final int MAXIMUM_BACKLIGHT = android.os.PowerManager.BRIGHTNESS_ON;
    private static final String SETTING_TOUCHPAD_STATUS = "touchpad_status";

    public static final String PERMISSION_KEYPAD_RECEIVER =
            "com.cyanogenmod.asusdec.permission.KEYPAD_RECEIVER";

    public static final String ACTION_DOCK_KEYPAD_KEY_PRESSED =
                                "com.cyanogenmod.asusdec.actions.ACTION_DOCK_KEYPAD_KEY_PRESSED";
    public static final String EXTRA_ASUSDEC_KEY = "key";
    public static final String EXTRA_ASUSDEC_STATUS = "status";
    public static final String EXTRA_ASUSDEC_VALUE = "value";

    // Private asusdec keys values (for notification purpose)
    public static final int ASUSDEC_UNKNOWN          = -1;
    public static final int ASUSDEC_WIFI             =  1;
    public static final int ASUSDEC_BT               =  2;
    public static final int ASUSDEC_TOUCHPAD         =  3;
    public static final int ASUSDEC_BRIGHTNESS       =  4;
    public static final int ASUSDEC_VOLUME           =  5;
    public static final int ASUSDEC_VOLUME_MUTE      =  6;
    public static final int ASUSDEC_MEDIA            =  7;
    public static final int ASUSDEC_SCREENSHOT       =  8;
    public static final int ASUSDEC_EXPLORER         =  9;
    public static final int ASUSDEC_SETTINGS         = 10;
    public static final int ASUSDEC_CAPS_LOCK        = 11;

    public static final int ASUSDEC_STATUS_OFF       =  0;
    public static final int ASUSDEC_STATUS_ON        =  1;

    public static final int ASUSDEC_MEDIA_PLAY_PAUSE =  0;
    public static final int ASUSDEC_MEDIA_PREVIOUS   =  1;
    public static final int ASUSDEC_MEDIA_NEXT       =  2;

    // Use specific scan codes from device instead of aosp keycodes
    private static final int SCANCODE_F1     = 59;
    private static final int SCANCODE_F2     = 60;
    private static final int SCANCODE_F3     = 61;
    private static final int SCANCODE_F4     = 62;
    private static final int SCANCODE_F5     = 63;
    private static final int SCANCODE_F6     = 64;
    private static final int SCANCODE_F7     = 65;
    private static final int SCANCODE_F8     = 66;
    private static final int SCANCODE_F9     = 67;
    private static final int SCANCODE_F10    = 68;
    private static final int SCANCODE_F11    = 87;
    private static final int SCANCODE_F12    = 88;
    private static final int SCANCODE_TOGGLE_WIFI     = 238;
    private static final int SCANCODE_TOGGLE_BT       = 237;
    private static final int SCANCODE_TOGGLE_TOUCHPAD = 202;
    private static final int SCANCODE_BRIGHTNESS_DOWN = 224;
    private static final int SCANCODE_BRIGHTNESS_UP   = 225;
    private static final int SCANCODE_BRIGHTNESS_AUTO = 244;
    private static final int SCANCODE_SCREENSHOT      = 212;
    private static final int SCANCODE_EXPLORER        = 150;
    private static final int SCANCODE_SETTINGS        = 141;
    private static final int SCANCODE_VOLUME_MUTE     = 113;
    private static final int SCANCODE_VOLUME_DOWN     = 114;
    private static final int SCANCODE_VOLUME_UP       = 115;
    private static final int SCANCODE_MEDIA_PREVIOUS  = 165;
    private static final int SCANCODE_MEDIA_PLAY_PAUSE = 164;
    private static final int SCANCODE_MEDIA_NEXT      = 163;
    private static final int SCANCODE_CAPS_LOCK       = 58;

    private static final int[] ALL_HANDLED_SCANCODES = {
        SCANCODE_F1, SCANCODE_F2, SCANCODE_F3, SCANCODE_F4, SCANCODE_F5, SCANCODE_F6,
        SCANCODE_F7, SCANCODE_F8, SCANCODE_F9, SCANCODE_F10, SCANCODE_F11, SCANCODE_F12,
        SCANCODE_TOGGLE_WIFI, SCANCODE_TOGGLE_BT, SCANCODE_TOGGLE_TOUCHPAD,
        SCANCODE_BRIGHTNESS_DOWN, SCANCODE_BRIGHTNESS_UP, SCANCODE_BRIGHTNESS_AUTO,
        SCANCODE_SCREENSHOT, SCANCODE_EXPLORER, SCANCODE_SETTINGS,
        SCANCODE_MEDIA_PREVIOUS, SCANCODE_MEDIA_PLAY_PAUSE, SCANCODE_MEDIA_NEXT,
        SCANCODE_VOLUME_MUTE, SCANCODE_VOLUME_DOWN, SCANCODE_VOLUME_UP,
        SCANCODE_CAPS_LOCK
    };

    private final Context mContext;
    private final Handler mHandler;
    private final Intent mSettingsIntent;
    private final boolean mAutomaticAvailable;
    private boolean mDocked = false;
    private boolean mTouchpadEnabled = true;
    private WifiManager mWifiManager;
    private AudioManager mAudioManager;
    private BluetoothAdapter mBluetoothAdapter;
    private IPowerManager mPowerManager;

    static {
        AsusdecNative.loadAsusdecLib();
    }

    public KeyHandler(Context context) {
        mContext = context;
        mHandler = new Handler();

        mSettingsIntent = new Intent(Intent.ACTION_MAIN, null);
        mSettingsIntent.setAction(Settings.ACTION_SETTINGS);
        mSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        mAutomaticAvailable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);

        try {
            if (Settings.Secure.getInt(mContext.getContentResolver(),
                    SETTING_TOUCHPAD_STATUS) == 0) {
                mTouchpadEnabled = false;
                nativeToggleTouchpad(false);
            }
        } catch (SettingNotFoundException e) {
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DOCK_EVENT);
        context.registerReceiver(mDockReceiver, filter);
    }

    BroadcastReceiver mDockReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_DOCK_EVENT.equals(intent.getAction())) {
                int dockMode = intent.getIntExtra(Intent.EXTRA_DOCK_STATE,
                        Intent.EXTRA_DOCK_STATE_UNDOCKED);
                mDocked = dockMode != Intent.EXTRA_DOCK_STATE_UNDOCKED;
                if (mDocked) {
                    nativeToggleTouchpad(mTouchpadEnabled);
                }
            }
        }
    };

    @Override
    public boolean handleKeyEvent(KeyEvent event) {

        boolean consumed = false;

        if (DEBUG) {
            Log.d(TAG, "KeyEvent: action=" + event.getAction()
                    + ", flags=" + event.getFlags()
                    + ", canceled=" + event.isCanceled()
                    + ", keyCode=" + event.getKeyCode()
                    + ", deviceId=" + event.getDeviceId()
                    + ", scanCode=" + event.getScanCode()
                    + ", metaState=" + event.getMetaState()
                    + ", repeatCount=" + event.getRepeatCount());
        }

        // Asusdec should only handle key events when the device is docked.
        // The device should be a full keyboard like the Transformers one (otherwise we
        // are in present of a gpio interruption)
        // TODO The check for ALPHA need to be verified (on TF700T ALPHA is mapped to gpio
        // driver (power and volume keys))
        if (!mDocked || event.getDeviceId() == KeyCharacterMap.ALPHA) {
            // No consume any key
            return false;
        }

        // Are we able to handle it?
        int index = isHandled(event);
        if (index == -1) {
            // Then no consume the key
            return false;
        }

        // Consume the event if we are going to handle it. We don't want other subsystem can
        // handle it)
        if (event.getAction() != getScanCodeAction(event) || event.getRepeatCount() != 0) {
            // Then mark as consumed
            return true;
        }

        // Now check every type of scancode that we are able to handle
        switch (event.getScanCode()) {
            case SCANCODE_TOGGLE_WIFI:
                toggleWifi();
                consumed = true;
                break;
            case SCANCODE_TOGGLE_BT:
                toggleBluetooth();
                consumed = true;
                break;
            case SCANCODE_TOGGLE_TOUCHPAD:
                toggleTouchpad();
                consumed = true;
                break;
            case SCANCODE_BRIGHTNESS_DOWN:
                brightnessDown();
                consumed = true;
                break;
            case SCANCODE_BRIGHTNESS_UP:
                brightnessUp();
                consumed = true;
                break;
            case SCANCODE_BRIGHTNESS_AUTO:
                toggleAutoBrightness();
                consumed = true;
                break;
            case SCANCODE_SCREENSHOT:
                takeScreenshot();
                consumed = true;
                break;
            case SCANCODE_EXPLORER:
                launchExplorer();
                // We only display a notification. Explorer is open by the framework
                consumed = false;
                break;
            case SCANCODE_SETTINGS:
                launchSettings();
                consumed = true;
                break;
            case SCANCODE_VOLUME_MUTE:
                // KEYCODE_VOLUME_MUTE is part of the aosp keyevent intercept handling, but
                // aosp uses it stop ringing in phone devices (no system volume mute toggle).
                // Since transformer devices doesn't have a telephony subsystem, we handle and
                // treat this event as a volume mute toggle action. the asusdec KeyHandler
                // mustn't mark the key event as consumed.
                toggleAudioMute();
                consumed = false;
                break;
            case SCANCODE_VOLUME_DOWN:
                volumeDown();
                consumed = false;
                break;
            case SCANCODE_VOLUME_UP:
                volumeUp();
                consumed = false;
                break;
            case SCANCODE_MEDIA_PLAY_PAUSE:
                mediaPlayPause();
                consumed = false;
                break;
            case SCANCODE_MEDIA_PREVIOUS:
                mediaPrevious();
                consumed = false;
                break;
            case SCANCODE_MEDIA_NEXT:
                mediaNext();
                consumed = false;
                break;
            case SCANCODE_CAPS_LOCK:
                capsLock(event);
                // We only display a notification. Explorer is open by the framework
                consumed = false;
                break;
        }

        // Return if the key was consumed here
        return consumed;
    }

    private int isHandled(final KeyEvent event) {
        int scancode = event.getScanCode();
        int cc = ALL_HANDLED_SCANCODES.length;
        for (int i = 0; i < cc; i++) {
            if (ALL_HANDLED_SCANCODES[i] == scancode){
                return i;
            }
        }
        return -1;
    }

    private int getScanCodeAction(final KeyEvent event) {
        int scancode = event.getScanCode();
        switch (scancode) {
            case SCANCODE_EXPLORER:
            case SCANCODE_VOLUME_MUTE:
            case SCANCODE_VOLUME_DOWN:
            case SCANCODE_VOLUME_UP:
            case SCANCODE_MEDIA_PLAY_PAUSE:
            case SCANCODE_MEDIA_PREVIOUS:
            case SCANCODE_MEDIA_NEXT:
            case SCANCODE_CAPS_LOCK:
                return KeyEvent.ACTION_DOWN;
            default:
                return KeyEvent.ACTION_UP;
        }
    }

    private void toggleWifi() {
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        }

        int state = mWifiManager.getWifiState();
        int apState = mWifiManager.getWifiApState();

        if (state == WifiManager.WIFI_STATE_ENABLING
                || state == WifiManager.WIFI_STATE_DISABLING) {
            return;
        }
        if (apState == WifiManager.WIFI_AP_STATE_ENABLING
                || apState == WifiManager.WIFI_AP_STATE_DISABLING) {
            return;
        }

        if (state == WifiManager.WIFI_STATE_ENABLED
                || apState == WifiManager.WIFI_AP_STATE_ENABLED) {
            mWifiManager.setWifiEnabled(false);
            mWifiManager.setWifiApEnabled(null, false);
            notifyKey(ASUSDEC_WIFI, ASUSDEC_STATUS_OFF);

        } else if (state == WifiManager.WIFI_STATE_DISABLED
                && apState == WifiManager.WIFI_AP_STATE_DISABLED) {
            mWifiManager.setWifiEnabled(true);
            notifyKey(ASUSDEC_WIFI, ASUSDEC_STATUS_ON);
        }
    }

    private void toggleBluetooth() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        int state = mBluetoothAdapter.getState();

        if (state == BluetoothAdapter.STATE_TURNING_OFF
                || state == BluetoothAdapter.STATE_TURNING_ON) {
            return;
        }
        if (state == BluetoothAdapter.STATE_OFF) {
            mBluetoothAdapter.enable();
            notifyKey(ASUSDEC_BT, ASUSDEC_STATUS_ON);
        }
        if (state == BluetoothAdapter.STATE_ON) {
            mBluetoothAdapter.disable();
            notifyKey(ASUSDEC_BT, ASUSDEC_STATUS_OFF);
        }
    }

    private void toggleTouchpad() {
        mTouchpadEnabled = !mTouchpadEnabled;
        nativeToggleTouchpad(mTouchpadEnabled);

        int enabled = mTouchpadEnabled ? 1 : 0;
        Settings.Secure.putInt(mContext.getContentResolver(),
                SETTING_TOUCHPAD_STATUS, enabled);

        notifyKey(ASUSDEC_TOUCHPAD, mTouchpadEnabled ? ASUSDEC_STATUS_ON : ASUSDEC_STATUS_OFF);
    }

    private void brightnessDown() {
        int value = getBrightness(MINIMUM_BACKLIGHT);
        value -= 10;
        if (value < MINIMUM_BACKLIGHT) {
            value = MINIMUM_BACKLIGHT;
        }
        setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        setBrightness(value);
        notifyKey(ASUSDEC_BRIGHTNESS, ASUSDEC_STATUS_OFF, value);
    }

    private void brightnessUp() {
        int value = getBrightness(MAXIMUM_BACKLIGHT);
        value += 10;
        if (value > MAXIMUM_BACKLIGHT) {
            value = MAXIMUM_BACKLIGHT;
        }
        setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        setBrightness(value);
        notifyKey(ASUSDEC_BRIGHTNESS, ASUSDEC_STATUS_OFF, value);
    }

    private void toggleAutoBrightness() {
        if (!mAutomaticAvailable) {
            return;
        }
        int value = getBrightness(MINIMUM_BACKLIGHT);
        int currentValue =
                Settings.System.getInt(
                    mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        setBrightnessMode(
                currentValue == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL ?
                                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC :
                                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        int status = currentValue == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL ?
                                    ASUSDEC_STATUS_ON :
                                    ASUSDEC_STATUS_OFF;
        notifyKey(ASUSDEC_BRIGHTNESS, status, value);
    }

    private void setBrightnessMode(int mode) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
    }

    private void setBrightness(int value) {
        if (mPowerManager == null) {
            mPowerManager = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));
        }
        try {
            mPowerManager.setTemporaryScreenBrightnessSettingOverride(value);
        } catch (RemoteException ex) {
            Slog.e(TAG, "Could not set backlight brightness", ex);
        }
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, value);
    }

    private int getBrightness(int def) {
        int value = def;
        try {
            value = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException ex) {
        }
        return value;
    }

    private void launchExplorer() {
        notifyKey(ASUSDEC_EXPLORER, ASUSDEC_STATUS_OFF);
    }

    private void launchSettings() {
        try {
            mContext.startActivity(mSettingsIntent);
            notifyKey(ASUSDEC_SETTINGS, ASUSDEC_STATUS_OFF);
        } catch (ActivityNotFoundException ex) {
            Slog.e(TAG, "Could not launch settings intent", ex);
        }
    }

    private void toggleAudioMute() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        }
        // We only act in normal mode (rings, calls, ... are handled by aosp)
        if (mAudioManager.getMode() == AudioManager.MODE_NORMAL) {
            // TODO: If an alarm is sound then don't toggle the volume mute. In this case,
            // is better to ignore the key event and let the alarm app to handle it.

            // Just toggle between normal and silent (by now we are not going to handle
            // vibration here)
            int newValue =
                    mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL ?
                    AudioManager.RINGER_MODE_NORMAL :
                    AudioManager.RINGER_MODE_SILENT;
            mAudioManager.setRingerMode(newValue);

            int status = newValue == AudioManager.RINGER_MODE_NORMAL ?
                    ASUSDEC_STATUS_OFF :
                    ASUSDEC_STATUS_ON;
            notifyKey(ASUSDEC_VOLUME_MUTE, status);
        }
    }

    private void volumeDown() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        }
        int value = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
        int status = mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL ?
                    ASUSDEC_STATUS_ON :
                    ASUSDEC_STATUS_OFF;
        notifyKey(ASUSDEC_VOLUME, status, value);
    }

    private void volumeUp() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        }
        int value = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
        int status = mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL ?
                    ASUSDEC_STATUS_ON :
                    ASUSDEC_STATUS_OFF;
        notifyKey(ASUSDEC_VOLUME, status, value);
    }

    private void mediaPlayPause() {
        notifyKey(ASUSDEC_MEDIA, ASUSDEC_MEDIA_PLAY_PAUSE);
    }

    private void mediaPrevious() {
        notifyKey(ASUSDEC_MEDIA, ASUSDEC_MEDIA_PREVIOUS);
    }

    private void mediaNext() {
        notifyKey(ASUSDEC_MEDIA, ASUSDEC_MEDIA_NEXT);
    }

    private void capsLock(KeyEvent event) {
        int status = event.getMetaState() == KeyEvent.META_CAPS_LOCK_ON ?
                ASUSDEC_STATUS_ON :
                ASUSDEC_STATUS_OFF;
        notifyKey(ASUSDEC_CAPS_LOCK, status);
    }

    /*
     * Screenshot stuff kanged from PhoneWindowManager
     */

    final Object mScreenshotLock = new Object();
    ServiceConnection mScreenshotConnection = null;

    final Runnable mScreenshotTimeout = new Runnable() {
        @Override
        public void run() {
            synchronized (mScreenshotLock) {
                if (mScreenshotConnection != null) {
                    mContext.unbindService(mScreenshotConnection);
                    mScreenshotConnection = null;
                }
            }
        }
    };

    // Assume this is called from the Handler thread.
    private void takeScreenshot() {
        synchronized (mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return;
            }

            notifyKey(ASUSDEC_SCREENSHOT, ASUSDEC_STATUS_OFF);

            ComponentName cn = new ComponentName("com.android.systemui",
                    "com.android.systemui.screenshot.TakeScreenshotService");
            Intent intent = new Intent();
            intent.setComponent(cn);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, 1);
                        final ServiceConnection myConn = this;
                        Handler h = new Handler(mHandler.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (mScreenshotConnection == myConn) {
                                        mContext.unbindService(mScreenshotConnection);
                                        mScreenshotConnection = null;
                                        mHandler.removeCallbacks(mScreenshotTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        msg.arg1 = msg.arg2 = 0;
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };
            if (mContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)) {
                mScreenshotConnection = conn;
                mHandler.postDelayed(mScreenshotTimeout, 10000);
            }
        }
    }

    private void notifyKey(int asusdeckey, int status, int value) {
        Intent intent = new Intent();
        intent.setAction(ACTION_DOCK_KEYPAD_KEY_PRESSED);
        intent.putExtra(EXTRA_ASUSDEC_KEY, asusdeckey);
        intent.putExtra(EXTRA_ASUSDEC_STATUS, status);
        intent.putExtra(EXTRA_ASUSDEC_VALUE, value);
        mContext.sendBroadcast(intent, PERMISSION_KEYPAD_RECEIVER);
    }

    private void notifyKey(int asusdeckey, int status) {
        notifyKey(asusdeckey, status, 0);
    }

    /*
     * ------------------------------------------------------------------------
     * Native methods
     */
    native private static boolean nativeToggleTouchpad(boolean status);
}
