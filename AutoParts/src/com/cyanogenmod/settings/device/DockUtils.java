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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cyanogenmod.asusdec.KeyHandler;

public class DockUtils {

    public static final String PREFERENCE_DOCK_EC_WAKEUP = "dock_ec_wakeup";
    public static final String PREFERENCE_DOCK_KP_FUNCTION_KEYS = "dock_kp_function_keys";
    public static final String PREFERENCE_DOCK_KP_NOTIFICATIONS = "dock_kp_notifications";

    static boolean getEcWakeUp(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFERENCE_DOCK_EC_WAKEUP, false);
    }

    static Set<String> getKpFunctionKeys(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> defaultValues = new HashSet<String>();
        return prefs.getStringSet(PREFERENCE_DOCK_KP_FUNCTION_KEYS, defaultValues);
    }

    static boolean getKpNotifications(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFERENCE_DOCK_KP_NOTIFICATIONS, false);
    }

    static long translateFunctionKeysToMetaKeys(Set<String> functionKeys) {
        String[] curValues = functionKeys.toArray(new String[functionKeys.size()]);
        long value = 0;
        int cc = curValues.length;
        for (int i = 0; i < cc; i++) {
            value += Long.parseLong(curValues[i]);
        }
        return value;
    }

    static String translateFunctionKeysToString(long functionKeys) {
        List<String> keys = new ArrayList<String>();
        if ((functionKeys & KeyEvent.META_SHIFT_MASK) == KeyEvent.META_SHIFT_MASK) {
            keys.add("SHIFT");
        }
        if ((functionKeys & KeyEvent.META_CTRL_MASK) == KeyEvent.META_CTRL_MASK) {
            keys.add("CTRL");
        }
        if ((functionKeys & KeyEvent.META_ALT_MASK) == KeyEvent.META_ALT_MASK) {
            keys.add("ALT");
        }
        if (keys.size() == 0) {
            return "-";
        }
        return TextUtils.join(" + ", keys);
    }

    static void setMetaFunctionKeys(long meta) {
        SystemProperties.set(KeyHandler.META_FUNCTION_KEYS_PROPERTY, String.valueOf(meta));
    }
}
