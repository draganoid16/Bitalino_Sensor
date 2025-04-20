package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import java.io.OutputStream;

public class AcquireService extends JobIntentService {

    private static final String TAG    = "AcquireService";
    private static final int    JOB_ID = 123;

    public static final String EXTRA_TYPE     = "type";
    public static final String EXTRA_MAC      = "mac";
    public static final String EXTRA_DURATION = "duration";   // seconds

    public static void enqueueWork(Context c, Intent i) {
        enqueueWork(c, AcquireService.class, JOB_ID, i);
    }

    @Override
    protected void onHandleWork(@NonNull Intent in) {
        var type = (ConnectorFactory.DeviceType) in.getSerializableExtra(EXTRA_TYPE);
        String mac = in.getStringExtra(EXTRA_MAC);
        int secs   = in.getIntExtra(EXTRA_DURATION, 5);

        if (type != ConnectorFactory.DeviceType.BITALINO) {
            Log.e(TAG, "Only BITalino handled here"); return;
        }

        BitalinoConnector conn = (BitalinoConnector) ConnectorFactory.create(type);
        conn.initialize(this);

        Handler ui = new Handler(Looper.getMainLooper());

        try {
            /* 1 ▸ connect on UI thread */
            ui.post(() -> { try { conn.connect(mac); } catch (Exception e){ Log.e(TAG,"connect",e);} });

            /* 2 ▸ wait until READY (max 8 s) */
            if (!conn.waitUntilReady(8_000)) {
                Log.e(TAG, "Device never became READY");
                return;

            }

            /* 3 ▸ start acquisition on UI thread */
            ui.postDelayed(() -> {
                try {
                    conn.start();
                } catch (Exception e) {
                    Log.e(TAG, "start()", e);
                }
            }, 250);

            /* 4 ▸ keep worker thread alive */
            Thread.sleep(secs * 1000L);

            /* 5 ▸ stop + disconnect on UI thread */
            ui.post(() -> {
                try {
                    conn.stop();
                    conn.disconnect();
                } catch (Exception e){ Log.e(TAG,"stop/disconnect",e); }
            });

            /* 6 ▸ persist data */
            saveToDownloads(conn.getCollectedData());
            Log.d(TAG, "Finished OK ("+secs+" s)");
        }
        catch (Exception e) { Log.e(TAG, "AcquireService fatal", e); }
    }

    /* ---------- save helper ------------------------------------------------ */

    private void saveToDownloads(String data) {
        if (data.isEmpty()) { Log.w(TAG,"No data captured"); return; }

        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Downloads.DISPLAY_NAME, "bitalino_data.txt");
            v.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                v.put(MediaStore.Downloads.RELATIVE_PATH, "Download/");

            Uri uri = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            }

            if (uri != null) try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                out.write(data.getBytes());
            }
            //show on UI
            new Handler(Looper.getMainLooper()).post(
                    () -> Toast.makeText(this,
                            "Saved data to Downloads/bitalino_data.txt",
                            Toast.LENGTH_LONG).show());

        } catch (Exception e) {
            Log.e(TAG, "saveToDownloads()", e);
        }
    }
}
