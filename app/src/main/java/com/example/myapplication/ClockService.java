package com.example.myapplication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

public class ClockService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        Python py = Python.getInstance();
        PyObject pyModule = py.getModule("clock_module");  // Your Python file
        pyModule.callAttr("service_started");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Python py = Python.getInstance();
        PyObject pyModule = py.getModule("clock_module");
        pyModule.callAttr("update_clock");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
