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

package org.cyanogenmod.hardware;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import dalvik.system.DexClassLoader;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Support for wake up the device by pressing any key of the dock keypad.
 */
public class DockKpWakeDevice {

    private static final String TAG = "KeyPadWakeUp";

    // So asusdec is a device, we need to call it through an ioctl which need
    // to be done by native code. While this code is executed at settings level
    // we are going to use the asusdec.jar library to call the low level.
    // That means load the asusdec.jar, get a reflect reference of the DockEmbeddedController
    // and then call to isECWakeUp and setECWakeUp methods

    private static final String ASUSDEC_LIBRARY = "/system/framework/com.cyanogenmod.asusdec.jar";

    private static final String ASUSDEC_DOCK_EMBEDDED_CONTROLLER_CLASS =
            "com.cyanogenmod.asusdec.DockEmbeddedController";

    private static final String EC_WAKE_UP_READ_METHOD = "isECWakeUp";
    private static final String EC_WAKE_UP_WRITE_METHOD = "setECWakeUp";

    private static Object mDockEmbeddedController;

    /**
     * Whether device supports wake up the device by pressing any key of the dock keypad.
     *
     * @return boolean Supported devices must return always true.
     */
    public static boolean isSupported(Context ctx) {
        final File library = new File(ASUSDEC_LIBRARY);
        return library.isFile() && loadAsusdecLibrary(ctx);
    }

    /**
     * Returns whether a dock keypad keypress event will wake up the device.
     *
     * @return boolean Must be false when is not supported or wake up is not enabled, or
     * when the operation failed while reading the status; true in any other case.
     */
    public static boolean isEnabled(Context ctx) {
        // Check that we hold a valid DockEmbeddedController reference 
        if (isSupported(ctx)) {
            try {
                final Class clazz = mDockEmbeddedController.getClass();
                Method method = clazz.getMethod(EC_WAKE_UP_READ_METHOD, new Class[]{});
                return ((Boolean)method.invoke(mDockEmbeddedController,
                        new Object[]{})).booleanValue();

            } catch (NoSuchMethodException ex) {
                Log.e(TAG, "Asusdec doesn't have ecWakeUp read method.");
                return false;
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, "Asusdec read method has a different signature.");
                return false;
            } catch (IllegalAccessException ex) {
                Log.e(TAG, "Process hasn't access to asusdec read method.");
                return false;
            } catch (InvocationTargetException ex) {
                Log.e(TAG, "Failed to instantiate the asusdec read method.");
                return false;
            }
        }
        return false;
    }

    /**
     * Sets whether a dock keypad keypress event will wake up the device.
     *
     * @param enabled The new value.
     * @return boolean Must be false if is not supported or the operation failed; true
     * in any other case.
     */
    public static boolean setEnabled(Context ctx, boolean enabled) {
        // Check that we hold a valid DockEmbeddedController reference
        if (isSupported(ctx)) {
            try {
                final Class clazz = mDockEmbeddedController.getClass();
                Method method = clazz.getMethod(EC_WAKE_UP_WRITE_METHOD,
                        new Class[]{boolean.class});
                return ((Boolean)method.invoke(mDockEmbeddedController,
                        new Object[]{enabled})).booleanValue();

            } catch (NoSuchMethodException ex) {
                Log.e(TAG, "Asusdec doesn't have ecWakeUp read method.");
                return false;
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, "Asusdec read method has a different signature.");
                return false;
            } catch (IllegalAccessException ex) {
                Log.e(TAG, "Process hasn't access to asusdec read method.");
                return false;
            } catch (InvocationTargetException ex) {
                Log.e(TAG, "Failed to instantiate the asusdec read method.");
                return false;
            }
        }
        return false;
    }

    // Loads the asusdec library and a DockEmbedded reference **/
    private synchronized static boolean loadAsusdecLibrary(Context ctx) {
        if (mDockEmbeddedController == null) {
            try {
                // Load a classloader from the asusdec library
                ClassLoader classLoader =
                        new DexClassLoader(
                                ASUSDEC_LIBRARY,
                                new ContextWrapper(ctx).getCacheDir().getAbsolutePath(),
                                null,
                                ClassLoader.getSystemClassLoader());

                // Load a new instance of the DockEmbeddedController class
                Class<?> clazz = classLoader.loadClass(ASUSDEC_DOCK_EMBEDDED_CONTROLLER_CLASS);
                mDockEmbeddedController = clazz.newInstance();

            } catch (ClassNotFoundException ex) {
                Log.e(TAG, "The DockEmbeddedController class wasn't found in the asusdec library.");
                return false;
            } catch (IllegalAccessException ex) {
                Log.e(TAG, "Process hasn't access to asusdec library.");
                return false;
            } catch (InstantiationException ex) {
                Log.e(TAG, "Failed to instantiate the asusdec library.");
                return false;
            }
        }
        return true;
    }
}
