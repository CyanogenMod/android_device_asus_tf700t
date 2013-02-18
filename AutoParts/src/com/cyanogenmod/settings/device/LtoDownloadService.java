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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cyanogenmod.settings.device.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class LtoDownloadService extends Service {
    private static final String TAG = "LtoDownloadService";
    private static final boolean ALOGV = true;

    private static final String LTO_SOURCE_URI_PATTERN = "http://gllto.glpals.com/%s/v2/latest/lto2.dat";
    private static final File LTO_DESTINATION_FILE = new File("/data/gps/lto.dat");

    public static final String KEY_ENABLED = "lto_download_enabled";
    public static final String KEY_FILE_TYPE = "lto_download_file";
    public static final String KEY_INTERVAL = "lto_download_interval";
    public static final String KEY_WIFI_ONLY = "lto_download_wifi_only";
    public static final String KEY_LAST_DOWNLOAD = "last_lto_download";

    public static final String EXTRA_FORCE_DOWNLOAD = "force_download";

    public static final String ACTION_STATE_CHANGE = "com.cyanogenmod.settings.device.LtoDownloadService.STATE_CHANGE";
    public static final String EXTRA_STATE = "state";
    public static final String EXTRA_SUCCESS = "success";
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_TIMESTAMP = "timestamp";
    public static final int STATE_IDLE = 0;
    public static final int STATE_DOWNLOADING = 1;

    private static final String FILE_TYPE_DEFAULT = "7day";
    private static final boolean WIFI_ONLY_DEFAULT = true;

    private static final int DOWNLOAD_TIMEOUT = 20000; /* 20 seconds */

    private LtoDownloadTask mTask;

    private NotificationManager mNotificationManager;
    private static final int NOTIFICATION_ID = R.string.lto_downloading_data_notification;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            if (ALOGV) Log.v(TAG, "LTO download is still active, not starting new download");
            return START_REDELIVER_INTENT;
        }

        boolean forceDownload = intent.getBooleanExtra(EXTRA_FORCE_DOWNLOAD, false);
        if (!shouldDownload(forceDownload)) {
            Log.d(TAG, "Service started, but shouldn't download ... stopping");
            stopSelf();
            return START_NOT_STICKY;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String type = prefs.getString(KEY_FILE_TYPE, FILE_TYPE_DEFAULT);
        String uri = String.format(LTO_SOURCE_URI_PATTERN, type);

        mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        mTask = new LtoDownloadTask(uri, LTO_DESTINATION_FILE);
        mTask.execute();

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            mTask.cancel(true);
            mTask = null;
        }
    }

    private boolean shouldDownload(boolean forceDownload) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        boolean hasConnection = false;

        if (info == null || !info.isConnected()) {
            if (ALOGV) Log.v(TAG, "No network connection is available for LTO download");
        } else if (forceDownload) {
            if (ALOGV) Log.v(TAG, "Download was forced, overriding network type check");
            hasConnection = true;
        } else {
            boolean wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, WIFI_ONLY_DEFAULT);
            if (wifiOnly && info.getType() != ConnectivityManager.TYPE_WIFI) {
                if (ALOGV) {
                    Log.v(TAG, "Active network is of type " +
                            info.getTypeName() + ", but Wifi only was selected");
                }
            } else {
                hasConnection = true;
            }
        }

        if (!hasConnection) {
            return false;
        }
        if (forceDownload) {
            return true;
        }
        if (!prefs.getBoolean(KEY_ENABLED, false)) {
            return false;
        }

        long now = System.currentTimeMillis();
        long lastDownload = LtoDownloadUtils.getLastDownload(this);
        long due = lastDownload + LtoDownloadUtils.getDownloadInterval(this);

        if (ALOGV) {
            Log.v(TAG, "Now " + now + " due " + due + "(" + new Date(due) + ")");
        }

        if (lastDownload != 0 && now < due) {
            if (ALOGV) Log.v(TAG, "LTO download is not due yet");
            return false;
        }

        return true;
    }

    private class LtoDownloadTask extends AsyncTask<Void, Integer, Integer> {
        private String mSource;
        private File mDestination;
        private File mTempFile;
        private WakeLock mWakeLock;

        private static final int RESULT_SUCCESS = 0;
        private static final int RESULT_FAILURE = 1;
        private static final int RESULT_CANCELLED = 2;

        public LtoDownloadTask(String source, File destination) {
            mSource = source;
            mDestination = destination;
            try {
                mTempFile = File.createTempFile("lto-download", null, getCacheDir());
            } catch (IOException e) {
                Log.w(TAG, "Could not create temporary file", e);
            }

            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }

        @Override
        protected void onPreExecute() {
            mWakeLock.acquire();
            reportStateChange(STATE_DOWNLOADING, null, null);
            showDownloadNotification(0, true);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            BufferedInputStream in = null;
            BufferedOutputStream out = null;
            int result = RESULT_SUCCESS;

            try {
                HttpParams httpParams = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParams, DOWNLOAD_TIMEOUT);
                HttpConnectionParams.setSoTimeout(httpParams, DOWNLOAD_TIMEOUT);

                HttpClient client = new DefaultHttpClient(httpParams);
                HttpGet request = new HttpGet();
                request.setURI(new URI(mSource));

                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();
                File outputFile = mTempFile != null ? mTempFile : mDestination;

                in = new BufferedInputStream(entity.getContent());
                out = new BufferedOutputStream(new FileOutputStream(outputFile));

                byte[] buffer = new byte[2048];
                int count, total = 0;
                long length = entity.getContentLength();

                while ((count = in.read(buffer, 0, buffer.length)) != -1) {
                    if (isCancelled()) {
                        result = RESULT_CANCELLED;
                        break;
                    }
                    out.write(buffer, 0, count);
                    total += count;

                    if (length > 0) {
                        float progress = (float) total * 100 / length;
                        publishProgress((int) progress);
                    }
                }

                Log.d(TAG, "Downloaded " + total + "/" + length + " bytes of LTO data");
                if (total == 0 || (length > 0 && total != length)) {
                    result = RESULT_FAILURE;
                }
                in.close();
                out.close();
            } catch (IOException e) {
                Log.w(TAG, "Failed downloading LTO data", e);
                result = RESULT_FAILURE;
            } catch (URISyntaxException e) {
                Log.e(TAG, "URI syntax wrong", e);
                result = RESULT_FAILURE;
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Log.d(TAG, "return " + result);
            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            reportStateChange(STATE_DOWNLOADING, null, progress[0]);
            showDownloadNotification(progress[0].intValue(), false);
        }

        @Override
        protected void onPostExecute(Integer result) {
            cancelDownloadNotificacion();
            if (result != null) {
                finish(result);
            }
        }

        @Override
        protected void onCancelled() {
            cancelDownloadNotificacion();
            finish(RESULT_CANCELLED);
        }

        private void finish(int result) {
            if (mTempFile != null) {
                if (result == RESULT_SUCCESS) {
                    mDestination.delete();
                    if (!mTempFile.renameTo(mDestination)) {
                        Log.w(TAG, "Could not move temporary file to destination");
                    } else {
                        mDestination.setReadable(true, false);
                    }
                }
                mTempFile.delete();
            } else if (result != RESULT_SUCCESS) {
                mDestination.delete();
            } else {
                mDestination.setReadable(true, false);
            }

            Context context = LtoDownloadService.this;

            if (result == RESULT_SUCCESS) {
                long now = System.currentTimeMillis();
                SharedPreferences.Editor editor =
                        PreferenceManager.getDefaultSharedPreferences(context).edit();

                editor.putLong(KEY_LAST_DOWNLOAD, now);
                editor.apply();

                LtoDownloadUtils.scheduleNextDownload(context, now);
            } else if (result == RESULT_FAILURE) {
                /* failure, schedule next download in 1 hour */
                long lastDownload = LtoDownloadUtils.getLastDownload(context);
                lastDownload += 60 * 60 * 1000;
                LtoDownloadUtils.scheduleNextDownload(context, lastDownload);
            } else {
                /* cancelled, likely due to lost network - we'll get restarted
                 * when network comes back */
            }

            reportStateChange(STATE_IDLE, result == RESULT_SUCCESS, null);
            mWakeLock.release();
            stopSelf();
        }
    }

    private void reportStateChange(int state, Boolean success, Integer progress) {
        Intent intent = new Intent(ACTION_STATE_CHANGE);
        intent.putExtra(EXTRA_STATE, state);
        if (success != null) {
            intent.putExtra(EXTRA_SUCCESS, success);
        }
        if (progress != null) {
            intent.putExtra(EXTRA_PROGRESS, progress);
        }
        intent.putExtra(EXTRA_TIMESTAMP, new Date().getTime());
        sendStickyBroadcast(intent);
    }

    private void showDownloadNotification(int progress, boolean indeterminate) {
        // Create a notification
        Bitmap largeIcon =
                (((BitmapDrawable)getResources().
                        getDrawable(
                                com.cyanogenmod.settings.device.R.drawable.stat_sys_download)).getBitmap());
        String title = getString(R.string.lto_downloading_data_notification);
        Notification.Builder builder = new Notification.Builder(this)
                        .setContentTitle(title)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setLargeIcon(largeIcon)
                        .setProgress(100, progress, indeterminate)
                        .setWhen(0);
        Notification notification = builder.build();
        notification.tickerText = title;
        notification.flags = Notification.FLAG_NO_CLEAR;
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void cancelDownloadNotificacion() {
        mNotificationManager.cancel(NOTIFICATION_ID);
    }
}
