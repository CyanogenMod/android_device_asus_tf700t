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

import android.os.SystemProperties;

public class CpuUtils {

    public static final String PREFERENCE_CPU_MODE = "cpu_settings";
    public static final String PREFERENCE_CPU_SET_ON_BOOT = "cpu_set_on_boot";

    public static final String CPU_SETTING_POWER_SAVE = "0";
    public static final String CPU_SETTING_BALANCED = "1";
    public static final String CPU_SETTING_PERFORMANCE = "2";

    public static final String DEFAULT_CPU_MODE = CPU_SETTING_BALANCED;

    private static final String CPU_PROPERTY = "sys.cpu.mode";

    public static String getCpuMode() {
        return SystemProperties.get(CPU_PROPERTY);
    }

    public static void setCpuMode(String mode) {
        SystemProperties.set(CPU_PROPERTY, mode);
    }
}
