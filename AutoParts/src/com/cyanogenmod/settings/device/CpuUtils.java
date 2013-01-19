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
