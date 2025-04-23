package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import info.plux.api.PLUXException;
import info.plux.api.bitalino.BITalinoCommunication;
import info.plux.api.bitalino.BITalinoCommunicationFactory;
import info.plux.api.bitalino.BITalinoFrame;
import info.plux.api.enums.TypeOfCommunication;
import info.plux.api.interfaces.Constants;
import info.plux.api.interfaces.OnDataAvailable;

/**
 * Connector for BITalino devices using the PLUX Android SDK.
 */
public class BitalinoConnector implements DeviceConnector, OnDataAvailable {

    private static final String TAG = "BitalinoConnector";

    /** will be decremented when ACTION_DEVICE_READY is fired */
    private final CountDownLatch readyLatch = new CountDownLatch(1);

    private Context               ctx;          // never null after initialize()
    private BITalinoCommunication api;          // PLUX communication instance
    private final StringBuilder   builder = new StringBuilder();

    /* ------------------------------------------------------------ life‑cycle */

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void initialize(Context context) {
        this.ctx = context;

        // Register for PLUX broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_STATE_CHANGED);
        filter.addAction(Constants.ACTION_DEVICE_READY);      // << key broadcast

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(pluxRx, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(pluxRx, filter);
        }
    }

    @Override
    public void connect(String mac) throws PLUXException {
        api = BITalinoCommunicationFactory.getCommunication(
                TypeOfCommunication.BLE,
                ctx,          // Context
                this          // OnDataAvailable callback
        );
        api.setConnectionControllerEnabled(false);
        api.setDataStreamControllerEnabled(true);

        api.connect(mac);
        Log.d(TAG, "connect() invoked for " + mac);
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(ctx, "Connected to BITalino: " + mac, Toast.LENGTH_LONG).show()
        );



    }

    @Override
    public void start() throws PLUXException {
        // Valid BLE frequencies are 1, 10, 50, 100 Hz.
        // Use max‑4 channels on older firmware.
        int samplingHz = 100;
        int[] channels = {0, 1, 2, 3};      // <= 4!

        api.start(samplingHz, channels);
        Log.d(TAG, "Acquisition started @" + samplingHz + " Hz, channels "
                + Arrays.toString(channels));
    }


    @Override
    public void stop() throws PLUXException {
        api.stop();
        Log.d(TAG, "Acquisition stopped");
    }

    @Override
    public void disconnect() throws PLUXException {
        if (api != null) api.disconnect();
        ctx.unregisterReceiver(pluxRx);
        Log.d(TAG, "Disconnected / receiver unregistered");
    }

    /* ------------------------------------------------------------ helpers   */

    /** Block the caller until DEVICE_READY or timeout */
    public boolean waitUntilReady(long timeoutMs) {
        try { return readyLatch.await(timeoutMs, TimeUnit.MILLISECONDS); }
        catch (InterruptedException ignore) { return false; }
    }

    /** Collected CSV‑like data (one line per BITalinoFrame) */
    public String getCollectedData() { return builder.toString(); }

    /* ------------------------------------------------------------ PLUX data */

    @Override
    public void onDataAvailable(Parcelable frame) {
        if (frame instanceof BITalinoFrame) {
            String line = frame.toString();
            builder.append(line).append('\n');
            Log.v(TAG, "Frame " + line);
        }
    }
    @Override public void onDataAvailable(String id,int seq,int[] data,int dig){}
    @Override public void onDataLost(String id,int cnt){}

    /* ------------------------------------------------------------ Broadcast */

    private final BroadcastReceiver pluxRx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String act = i.getAction();

            if (Constants.ACTION_DEVICE_READY.equals(act)) {
                Log.d(TAG, ">>> DEVICE_READY");
                readyLatch.countDown();                     // <‑‑ keep for newer FW
            }
            else if (Constants.ACTION_STATE_CHANGED.equals(act)) {
                Object st = i.getSerializableExtra(Constants.EXTRA_STATE_CHANGED);
                Log.d(TAG, "STATE_CHANGED = " + st);

            /* Some BITalino firmwares never fire DEVICE_READY, only a final
               CONNECTED state.  Treat that as “ready”. */
                if ("CONNECTED".equals(String.valueOf(st))) {
                    readyLatch.countDown();                 // <‑‑ new line
                }
            }
        }
    };
}
