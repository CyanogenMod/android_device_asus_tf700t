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

#define LOG_TAG "AsusdecDockEmbeddedController"

#include "JNIHelp.h"
#include "jni.h"
#include <utils/Log.h>
#include <utils/misc.h>

#include <android_runtime/AndroidRuntime.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/ioctl.h>

namespace asusdec {

#define ASUSDEC_DEV "/dev/asusdec"
#define ASUSDEC_SYSFS_EC_WAKEUP "/sys/class/power_supply/dock_battery/device/ec_wakeup"

// copied from drivers/input/asusec/asusdec.h
#define ASUSDEC_EC_ON   1
#define ASUSDEC_EC_OFF  0
#define ASUSDEC_IOC_MAGIC   0xf4
#define ASUSDEC_EC_WAKEUP   _IOR(ASUSDEC_IOC_MAGIC, 6, int)

static int readGpio(const char* path, char* buf, size_t size)
{
    if (!path)
        return -1;
    int fd = open(path, O_RDONLY, 0);
    if (fd == -1) {
        ALOGE("Could not open '%s'", path);
        return -1;
    }

    ssize_t count = read(fd, buf, size);
    if (count > 0) {
        while (count > 0 && buf[count-1] == '\n')
            count--;
        buf[count] = '\0';
    } else {
        buf[0] = '\0';
    }

    close(fd);
    return count;
}

JNIEXPORT jint JNICALL asusdec_DockEmbeddedController_nativeReadECWakeUp
    (JNIEnv *env, jclass cls) {

    const int SIZE = 16;
    char buf[SIZE];

    int count = readGpio(ASUSDEC_SYSFS_EC_WAKEUP, buf, SIZE);
    int ret = -1;
    if (count > 0) {
        ret = atoi(buf);
    }

    return (jint)ret;
}

JNIEXPORT jboolean JNICALL asusdec_DockEmbeddedController_nativeWriteECWakeUp
    (JNIEnv *env, jclass cls, jboolean on) {

    int fd = open(ASUSDEC_DEV, O_RDONLY | O_NONBLOCK);
    if (fd < 0) {
        ALOGE("Could not open '%s'", ASUSDEC_DEV);
        return (jboolean)false;
    }

    int flag = (on) ? ASUSDEC_EC_ON : ASUSDEC_EC_OFF;
    int ret = ioctl(fd, ASUSDEC_EC_WAKEUP, flag);
    if (ret != 0) {
        ALOGE("Error calling ioctl ECWakeUp, %d\n", ret);
    }
    else {
        ALOGD("ECWakeUp is now %s\n", ((flag) ? "on" : "off"));
    }

    close(fd);

    return (jboolean) (ret == 0 ? true : false);
}

static JNINativeMethod sMethods[] = {
    /* name, signature, funcPtr */
    {"nativeReadECWakeUp", "()I", (void*)asusdec_DockEmbeddedController_nativeReadECWakeUp},
    {"nativeWriteECWakeUp", "(Z)Z", (void*)asusdec_DockEmbeddedController_nativeWriteECWakeUp},
};

int register_asusdec_DockEmbeddedController(JNIEnv* env)
{
    return jniRegisterNativeMethods(env, "com/cyanogenmod/asusdec/DockEmbeddedController", sMethods, NELEM(sMethods));
}

} /* namespace asusdec */
