LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_SRC_FILES := libfuse-lite/fuse.c \
	libfuse-lite/fuse_kern_chan.c \
	libfuse-lite/fuse_loop.c \
	libfuse-lite/fuse_lowlevel.c \
	libfuse-lite/fuse_opt.c \
	libfuse-lite/fuse_session.c \
	libfuse-lite/fuse_signals.c \
	libfuse-lite/fusermount.c \
	libfuse-lite/helper.c \
	libfuse-lite/mount.c \
	libfuse-lite/mount_util.c \
	libntfs-3g/acls.c \
	libntfs-3g/attrib.c \
	libntfs-3g/attrlist.c \
	libntfs-3g/bitmap.c \
	libntfs-3g/bootsect.c \
	libntfs-3g/cache.c \
	libntfs-3g/collate.c \
	libntfs-3g/compat.c \
	libntfs-3g/compress.c \
	libntfs-3g/debug.c \
	libntfs-3g/device.c \
	libntfs-3g/dir.c \
	libntfs-3g/index.c \
	libntfs-3g/inode.c \
	libntfs-3g/lcnalloc.c \
	libntfs-3g/logfile.c \
	libntfs-3g/logging.c \
	libntfs-3g/mft.c \
	libntfs-3g/misc.c \
	libntfs-3g/mst.c \
	libntfs-3g/object_id.c \
	libntfs-3g/realpath.c \
	libntfs-3g/reparse.c \
	libntfs-3g/runlist.c \
	libntfs-3g/security.c \
	libntfs-3g/unistr.c \
	libntfs-3g/volume.c \
	libntfs-3g/unix_io.c 

LOCAL_C_INCLUDES := $(LOCAL_PATH) $(LOCAL_PATH)/include/fuse-lite $(LOCAL_PATH)/include/ntfs-3g $(LOCAL_PATH)/libfuse-lite
LOCAL_MODULE:= libntfs-3g
LOCAL_CFLAGS += -DHAVE_CONFIG_H
LOCAL_CFLAGS += -D_LARGEFILE_SOURCE -D_LARGEFILE64_SOURCE -D_FILE_OFFSET_BITS=64
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS := optional
include $(BUILD_SHARED_LIBRARY)



include $(CLEAR_VARS)
LOCAL_MODULE := ntfs-3g
LOCAL_SRC_FILES := src/ntfs-3g.c \
                   src/ntfs-3g_common.c
LOCAL_C_INCLUDES := $(LOCAL_PATH) $(LOCAL_PATH)/include/fuse-lite $(LOCAL_PATH)/include/ntfs-3g $(LOCAL_PATH)/libfuse-lite
LOCAL_SHARED_LIBRARIES := libntfs-3g
LOCAL_CFLAGS += -D_LARGEFILE_SOURCE -D_LARGEFILE64_SOURCE -D_FILE_OFFSET_BITS=64 -DHAVE_ERRNO_H -DHAVE_CONFIG_H
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS := optional
include $(BUILD_EXECUTABLE)


include $(CLEAR_VARS)
LOCAL_MODULE := ntfs-3g.probe
LOCAL_SRC_FILES := src/ntfs-3g.probe.c
LOCAL_C_INCLUDES := $(LOCAL_PATH) $(LOCAL_PATH)/include/ntfs-3g $(LOCAL_PATH)/libfuse-lite
LOCAL_SHARED_LIBRARIES := libntfs-3g
LOCAL_CFLAGS += -D_LARGEFILE_SOURCE -D_LARGEFILE64_SOURCE -D_FILE_OFFSET_BITS=64
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS := optional
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_MODULE := ntfsfix
LOCAL_SRC_FILES := ntfsprogs/ntfsfix.c ntfsprogs/utils.c
LOCAL_C_INCLUDES := $(LOCAL_PATH) $(LOCAL_PATH)/include/fuse-lite $(LOCAL_PATH)/include/ntfs-3g $(LOCAL_PATH)/libfuse-lite
LOCAL_SHARED_LIBRARIES := libntfs-3g
LOCAL_CFLAGS += -D_LARGEFILE_SOURCE -D_LARGEFILE64_SOURCE -D_FILE_OFFSET_BITS=64 -DHAVE_ERRNO_H -DHAVE_CONFIG_H
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS := optional
include $(BUILD_EXECUTABLE)
