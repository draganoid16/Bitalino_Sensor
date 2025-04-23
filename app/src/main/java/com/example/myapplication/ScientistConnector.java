package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class ScientistConnector implements DeviceConnector {
    private static final String TAG = "ScientisstConnector";
    private Context context;
    private String mac;
    private double durationSecs;
    private String latestData;            // <-- store the result here

    /** Must be set before start() */
    public void setDuration(double secs) {
        this.durationSecs = secs;
    }

    @Override
    public void initialize(Context ctx) {
        this.context = ctx;
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(ctx));
        }
    }

    @Override
    public void connect(String mac) {
        this.mac = mac;
        Log.d(TAG, "ScientISSTConnector: MAC=" + mac);
    }

    @Override
    public void start() {
        Python py = Python.getInstance();
        PyObject mod = py.getModule("scientisst_service");
        PyObject result = mod.callAttr(
                "start_scientisst_service",
                mac,
                1000,                                  // sampling rate
                py.getBuiltins().get("list").call(1,2,3,4,5,6), // channels
                durationSecs
        );
        latestData = result.toString();  // buffer it
    }

    @Override
    public void stop() {
        // nothing to do: the Python wrapper already stops after duration
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "ScientISSTConnector: disconnected");
    }

    /** exposed so AcquireService can save it **/
    public String getCollectedData() {
        return latestData != null ? latestData : "";
    }
}

