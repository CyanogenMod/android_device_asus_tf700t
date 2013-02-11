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

package com.cyanogenmod.asusdec;

import android.util.Log;

/**
 * This is a private class that should be invoked by AsusPart only
 * @hide
 */
public final class DockEmbeddedController {
    private static final String TAG = "AsusdecDockEmbeddedController";

    private static int ASUSDEC_EC_ON = 1;
    private static int ASUSDEC_EC_OFF = 0;

    private boolean mECWakeUp;

    static {
        AsusdecNative.loadAsusdecLib();
    }

    public DockEmbeddedController() {
        readECWakeUpStatus();
    }

    private void readECWakeUpStatus() {
        boolean ecWakeUp = nativeReadECWakeUp() == ASUSDEC_EC_ON;
        mECWakeUp = ecWakeUp;
        Log.i(TAG, String.format("Readed EC WakeUp status: %s", mECWakeUp ? "on" : "off"));
    }

    public boolean isECWakeUp() {
        return mECWakeUp;
    }

    public boolean setECWakeUp(boolean on) {
        boolean ret = nativeWriteECWakeUp(on);
        if (ret){
            mECWakeUp = on;
        }
        return ret;
    }

    public boolean setECWakeUpAll(boolean on) {
        return true;
    }

    native private static int nativeReadECWakeUp();
    native private static boolean nativeWriteECWakeUp(boolean on);
}
