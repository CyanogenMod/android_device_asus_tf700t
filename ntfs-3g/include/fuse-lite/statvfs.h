#ifndef __PRIVATE_STATVFS_H
#define __PRIVATE_STATVFS_H
#include <asm/types.h>
struct statvfs
{
	unsigned long int f_bsize;
	unsigned long int f_frsize;
	__u64 f_blocks;
	__u64 f_bfree;
	__u64 f_bavail;
	__u64 f_files;
	__u64 f_ffree;
	__u64 f_favail;
	
	unsigned long int f_fsid;

	unsigned long int f_flag;
	unsigned long int f_namemax;
	int __f_spare[6];
};
#endif
