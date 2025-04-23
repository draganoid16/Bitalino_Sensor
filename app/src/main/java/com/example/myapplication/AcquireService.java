package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.OutputStream;

public class AcquireService extends Service {
    private static final String TAG = "AcquireService";

    public static final String ACTION_START = "com.example.myapplication.ACTION_START_CAPTURE";
    public static final String ACTION_STOP  = "com.example.myapplication.ACTION_STOP_CAPTURE";

    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_MAC  = "mac";
    public static final String EXTRA_USER = "userId";
    public static final String EXTRA_HDR  = "header";

    private DeviceConnector connector;
    private String         userId;
    private String         headerLine;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    "capture", "Data Capture", NotificationManager.IMPORTANCE_LOW
            );
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(chan);
        }
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if (ACTION_START.equals(action)) {
            ConnectorFactory.DeviceType type =
                    (ConnectorFactory.DeviceType) intent.getSerializableExtra(EXTRA_TYPE);
            String mac  = intent.getStringExtra(EXTRA_MAC);
            userId      = intent.getStringExtra(EXTRA_USER);
            headerLine  = intent.getStringExtra(EXTRA_HDR);

            // 1) Build and fire off our foreground notification
            Notification n = new NotificationCompat.Builder(this, "capture")
                    .setContentTitle("Capturing data…")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setOngoing(true)
                    .build();
            startForeground(42, n);

            // 2) Initialize connector and start acquisition on background thread
            connector = ConnectorFactory.create(type);
            connector.initialize(this);
            new Thread(() -> {
                try {
                    connector.connect(mac);
                    Thread.sleep(2_000);

                    connector.start();
                } catch (Exception e) {
                    Log.e(TAG, "capture start failed", e);
                    stopSelf();
                }
            }).start();

            return START_STICKY;
        }
        else if (ACTION_STOP.equals(action)) {
            // 3) Stop, disconnect, collect data, save, tear down
            new Thread(() -> {
                try {
                    connector.stop();
                    connector.disconnect();

                    String data = "";
                    if (connector instanceof BitalinoConnector) {
                        data = ((BitalinoConnector)connector).getCollectedData();
                    }
                    else if (connector instanceof ScientistConnector) {
                        data = ((ScientistConnector)connector).getCollectedData();
                    }

                    saveToDownloads(userId, headerLine + "\n" + data);

                } catch (Exception e) {
                    Log.e(TAG, "capture stop failed", e);
                } finally {
                    stopForeground(true);
                    stopSelf();
                }
            }).start();

            return START_NOT_STICKY;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Saves the CSV to Downloads/<userId>/bitalino_data.txt.
     */
    private void saveToDownloads(String userId, String csv) {
        if (csv == null || csv.isEmpty()) {
            Log.w(TAG, "No data to save");
            return;
        }
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Downloads.DISPLAY_NAME, "bitalino_data.txt");
            cv.put(MediaStore.Downloads.MIME_TYPE,    "text/plain");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Downloads/<userId>/
                cv.put(MediaStore.Downloads.RELATIVE_PATH, "Download/" + userId + "/");
            }
            Uri uri = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            }
            if (uri != null) {
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    os.write(("Timestamp:" + "\t" + "A1" +"\t" + "A2" + "\t" + "A3" + "\t" + "A4" + "\t" + "A5" + "\t" + "A6" + "\t" + "D1" + "\t" + "D2" + "\t" + "D3" + "\t" + "D4").getBytes("UTF-8"));
                    os.write(csv.getBytes("UTF-8"));
                }
                Log.d(TAG, "Saved to Downloads/" + userId + "/bitalino_data.txt");
            }
        } catch (Exception e) {
            Log.e(TAG, "saveToDownloads()", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (connector != null) {
            try { connector.disconnect(); }
            catch (Exception ignored) {}
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
