package com.example.myapplication;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQ_PERM = 2;

    // After picking:
    private ConnectorFactory.DeviceType selectedType;
    private String connectorMac;

    // user info
    private String currentUserId;
    private String csvHeaderLine;

    // launcher for DevicePickerActivity
    private final ActivityResultLauncher<Intent> pickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            connectorMac = result.getData()
                                    .getStringExtra("selected_device_mac");
                            if (connectorMac != null) showUserInfoDialog();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupEdgeToEdgeInsets();
        initPython();
        animateProgressBar();

        // choose which connector
        findViewById(R.id.bitalinoButton)
                .setOnClickListener(v -> chooseAndLaunch(ConnectorFactory.DeviceType.BITALINO));
        findViewById(R.id.scientisstButton)
                .setOnClickListener(v -> chooseAndLaunch(ConnectorFactory.DeviceType.SCIENTIST));

        // start / stop capture
        Button startBtn = findViewById(R.id.startCaptureButton);
        Button stopBtn = findViewById(R.id.stopCaptureButton);
        startBtn.setOnClickListener(v -> onStartCapture());
        stopBtn.setOnClickListener(v -> onStopCapture());
    }

    private void chooseAndLaunch(ConnectorFactory.DeviceType type) {
        selectedType = type;
        if (checkBtPermissions()) {
            pickerLauncher.launch(new Intent(this, DevicePickerActivity.class));
        }
    }

    private void onStartCapture() {
        if (connectorMac == null || currentUserId == null) {
            Toast.makeText(this,
                    "Pick a device and enter your user info first",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Intent svc = new Intent(this, AcquireService.class)
                .setAction(AcquireService.ACTION_START)
                .putExtra(AcquireService.EXTRA_TYPE, selectedType)
                .putExtra(AcquireService.EXTRA_MAC, connectorMac)
                .putExtra("userId", currentUserId)
                .putExtra("header", csvHeaderLine);
        ContextCompat.startForegroundService(this, svc);
        Toast.makeText(this, "Capturing…", Toast.LENGTH_SHORT).show();
    }

    private void onStopCapture() {
        Intent svc = new Intent(this, AcquireService.class).setAction(AcquireService.ACTION_STOP);
        // “start” the service with the STOP action so onStartCommand sees it:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, svc);
        } else {
            startService(svc);
        }
        Toast.makeText(this, "Capture stopped", Toast.LENGTH_SHORT).show();
    }

    /**
     * After picking MAC, ask for ID/age/gender/height/weight
     */
    private void showUserInfoDialog() {
        View form = getLayoutInflater()
                .inflate(R.layout.dialog_user_info, null, false);
        EditText etId = form.findViewById(R.id.etId);
        EditText etAge = form.findViewById(R.id.etAge);
        EditText etGender = form.findViewById(R.id.etGender);
        EditText etHeight = form.findViewById(R.id.etHeight);
        EditText etWeight = form.findViewById(R.id.etWeight);

        new AlertDialog.Builder(this)
                .setTitle("Enter user info")
                .setView(form)
                .setPositiveButton("OK", (d, w) -> {
                    currentUserId = etId.getText().toString().trim();
                    if (currentUserId.isEmpty()) {
                        Toast.makeText(this, "ID is required", Toast.LENGTH_LONG).show();
                        currentUserId = null;
                        return;
                    }
                    csvHeaderLine = "ID=" + currentUserId
                            + ",AGE=" + etAge.getText().toString().trim()
                            + ",GENDER=" + etGender.getText().toString().trim()
                            + ",HEIGHT=" + etHeight.getText().toString().trim()
                            + ",WEIGHT=" + etWeight.getText().toString().trim();

                    Toast.makeText(this,
                            "Ready! Tap START to begin capturing.",
                            Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean checkBtPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        boolean ok = ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        if (!ok) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT},
                    REQ_PERM);
        }
        return ok;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM
                && grantResults.length == 2
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            // retry
            pickerLauncher.launch(new Intent(this, DevicePickerActivity.class));
        }
    }

    private void setupEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.mainLayout),
                (v, insets) -> {
                    Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(b.left, b.top, b.right, b.bottom);
                    return insets;
                });
    }

    private void initPython() {
        if (!Python.isStarted()) Python.start(new AndroidPlatform(this));
    }

    private void animateProgressBar() {
        ObjectAnimator.ofInt(
                findViewById(R.id.semiCircularProgress),
                "progress", 0, 52
        ).setDuration(2000).start();
    }

    /**
     * Called by AcquireService when it has finished collecting data.
     * Creates / writes to Downloads/<userId>/bitalino_data.txt
     */
    public void saveToDownloads(String userId, String csv) {
        if (csv == null || csv.isEmpty()) return;
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Downloads.DISPLAY_NAME, "bitalino_data.txt");
            cv.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cv.put(MediaStore.Downloads.RELATIVE_PATH,
                        "Download/" + userId + "/");
            }
            Uri uri = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            }
            if (uri != null) try (OutputStream os =
                                          getContentResolver().openOutputStream(uri)) {
                os.write((csvHeaderLine + "\n").getBytes());
                os.write(csv.getBytes());
            }
            Toast.makeText(this,
                    "Saved to Downloads/" + userId + "/bitalino_data.txt",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "saveToDownloads()", e);
        }
    }
}
