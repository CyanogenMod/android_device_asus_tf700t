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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class DisplayUtils {

    public static final String PREFERENCE_DISPLAY_SMARTDIMMER = "display_smartdimmer";

    private static final String SMARTDIMMER_SYSFS =
            "/sys/class/graphics/fb0/device/smartdimmer/enable";

    static boolean getSmartdimmer(Context context) {
        // Enabled by default
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFERENCE_DISPLAY_SMARTDIMMER, true);
    }

    static boolean readSmartdimmerStatus() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(SMARTDIMMER_SYSFS));
            String value = br.readLine();
            return Integer.parseInt(value.trim()) == 1;
        } catch (Exception ex) {
            return false;
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (Exception ex) {/**NON BLOCK**/}
        }
    }

    static boolean writeSmartdimmerStatus(boolean enabled) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(SMARTDIMMER_SYSFS, false));
            bw.write(enabled ? "1" : "0");
        } catch (Exception ex) {
            return false;
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (Exception ex) {/**NON BLOCK**/}
        }
        return true;
    }
}
