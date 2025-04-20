package com.example.myapplication;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.PyObject;

import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG       = "MainActivity";
    private static final int    REQ_PICK  = 1;
    private static final int    REQ_PERM  = 2;

    private ConnectorFactory.DeviceType selectedType;
    private DeviceConnector connector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupEdgeToEdgeInsets();
        initializePython();
        animateProgressBar();
        setupSettingsButton();

        Button bitalinoBtn   = findViewById(R.id.bitalinoButton);
        Button scientisstBtn = findViewById(R.id.scientisstButton);

        bitalinoBtn.setOnClickListener(v -> chooseAndLaunch(ConnectorFactory.DeviceType.BITALINO));
        scientisstBtn.setOnClickListener(v -> chooseAndLaunch(ConnectorFactory.DeviceType.SCIENTIST));
    }

    private void chooseAndLaunch(ConnectorFactory.DeviceType type) {
        selectedType = type;
        if (checkPermissions()) {
            launchPicker();
        }
    }

    private void setupEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout), (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return insets;
        });
    }

    private void initializePython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }

    private void animateProgressBar() {
        ObjectAnimator.ofInt(
                findViewById(R.id.semiCircularProgress),
                "progress", 0, 52
        ).setDuration(2000).start();
    }

    private void setupSettingsButton() {
        findViewById(R.id.settingsButton)
                .setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private boolean checkPermissions() {
        boolean ok = true;
        // pre‑Android 12 needs LOCATION to discover
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            ok &= ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        // Android 12+ needs SCAN & CONNECT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ok &= ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        if (!ok) {
            String[] perms;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                perms = new String[]{ Manifest.permission.ACCESS_FINE_LOCATION };
            } else {
                perms = new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                };
            }
            ActivityCompat.requestPermissions(this, perms, REQ_PERM);
        }
        return ok;
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        if (req == REQ_PERM) {
            boolean granted = true;
            for (int r : results) if (r != PackageManager.PERMISSION_GRANTED) granted = false;
            if (granted && selectedType != null) {
                launchPicker();
            } else {
                Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(req, perms, results);
        }
    }

    private void launchPicker() {
        startActivityForResult(
                new Intent(this, DevicePickerActivity.class),
                REQ_PICK
        );
    }

    @Override
    protected void onActivityResult(int rc, int rs, @Nullable Intent data) {
        super.onActivityResult(rc, rs, data);
        if (rc == REQ_PICK && rs == RESULT_OK && data != null) {
            String mac = data.getStringExtra("selected_device_mac");
            if (mac != null) {
                connectAndRunWithPrompt(mac);
            } else {
                Toast.makeText(this, "No device selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void connectAndRunWithPrompt(String mac) {
        // 1) ask user for duration
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Seconds");

        new AlertDialog.Builder(this)
                .setTitle("Acquisition Duration")
                .setMessage("How many seconds should we record?")
                .setView(input)
                .setPositiveButton("Start", (dlg, which) -> {
                    // parse the seconds (default to 10 if invalid)
                    int secs = 10;
                    try {
                        secs = Integer.parseInt(input.getText().toString().trim());
                    } catch (NumberFormatException ignored) {}

                    // 2) build the work Intent
                    Intent work = new Intent(this, AcquireService.class);
                    work.putExtra(AcquireService.EXTRA_TYPE,     selectedType);
                    work.putExtra(AcquireService.EXTRA_MAC,      mac);
                    work.putExtra(AcquireService.EXTRA_DURATION, secs);

                    // 3) enqueue it
                    AcquireService.enqueueWork(this, work);

                    Toast.makeText(this,
                            "Recording in background for " + secs + "s",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    /** write out data. */
    public void saveToDownloads(String data) {
        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Downloads.DISPLAY_NAME, "device_data.txt");
            v.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.put(MediaStore.Downloads.RELATIVE_PATH, "Download/");
            } else {
                String p = Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        .getAbsolutePath();
                v.put(MediaStore.Downloads.DATA, p + "/device_data.txt");
            }
            Uri u = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            }
            try (OutputStream out = getContentResolver().openOutputStream(u)) {
                out.write(data.getBytes("UTF-8"));
            }
            Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Save error", e);
            Toast.makeText(this, "Save failed", Toast.LENGTH_LONG).show();
        }
    }

    /** Helper your Python-based connectors call. */
    public String runPythonService(String mac) {
        try {
            Python py = Python.getInstance();
            PyObject mod = py.getModule(
                    selectedType == ConnectorFactory.DeviceType.BITALINO
                            ? "bitalino" : "scientisst_service"
            );
            if (selectedType == ConnectorFactory.DeviceType.BITALINO) {
                return mod.callAttr("start_bitalino_service", mac).toString();
            } else {
                // for ScientISST connector, pass in (mac, rate, channels, duration) etc.
                return mod.callAttr("start_scientisst_service", mac).toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Python error", e);
            return "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connector != null) {
            try { connector.disconnect(); }
            catch (Exception ignored) {}
        }
    }
}
