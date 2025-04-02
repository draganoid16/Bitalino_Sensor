package com.example.myapplication;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_DEVICE_PICKER = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_PERMISSION_BLUETOOTH = 3;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Apply edge-to-edge insets.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Start Chaquopy Python if not already started.
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        // Animate the custom semi-circular progress bar.
        // Make sure your layout includes a view with ID "semiCircularProgress" of type SemiCircularProgressBar.
        SemiCircularProgressBar semiCircularProgress = findViewById(R.id.semiCircularProgress);
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(semiCircularProgress, "progress", 0, 52);
        progressAnimator.setDuration(2000); // Animation duration in milliseconds.
        progressAnimator.start();

        // Initialize the Bluetooth adapter.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e("MainActivity", "Bluetooth is not supported on this device.");
            Toast.makeText(this, "Bluetooth is not supported on this device.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check and request necessary permissions.
        if (!hasRequiredPermissions()) {
            requestRequiredPermissions();
        } else {
            checkBluetoothEnabled();
        }
    }

    /**
     * Checks if the required permissions are granted.
     */
    private boolean hasRequiredPermissions() {
        // For pre-Android 12 devices, check location permission.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        // For Android 12 and above, check new Bluetooth permissions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Requests the necessary permissions.
     */
    private void requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Request both legacy location permission and new Bluetooth permissions.
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                    },
                    REQUEST_PERMISSION_BLUETOOTH);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSION_BLUETOOTH);
        }
    }

    /**
     * Checks if Bluetooth is enabled. If not, requests the user to enable it.
     * If enabled, launches the device picker.
     */
    private void checkBluetoothEnabled() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // For Android 12+, check for BLUETOOTH_CONNECT permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Permissions will be requested in onRequestPermissionsResult.
                    return;
                }
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            launchDevicePicker();
        }
    }

    /**
     * Launches the system Bluetooth device picker.
     * Note: This uses an undocumented intent and may not work on all devices.
     */
    private void launchDevicePicker() {
        Intent intent = new Intent("android.bluetooth.devicepicker.action.LAUNCH");
        intent.putExtra("android.bluetooth.devicepicker.extra.NEED_AUTH", false);
        intent.putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 1); // 1 for classic Bluetooth devices.
        intent.putExtra("android.bluetooth.devicepicker.extra.SELECT_DISCOVERED", true);
        intent.putExtra("android.bluetooth.devicepicker.extra.LAUNCH_PACKAGE", getPackageName());
        intent.putExtra("android.bluetooth.devicepicker.extra.DEVICE_PICKER_LAUNCH_CLASS", MainActivity.class.getName());
        startActivityForResult(intent, REQUEST_CODE_DEVICE_PICKER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_BLUETOOTH) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                checkBluetoothEnabled();
            } else {
                Log.e("MainActivity", "Required Bluetooth permissions are not granted.");
                Toast.makeText(this, "Bluetooth permissions are required for the app to function.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle result for enabling Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                launchDevicePicker();
            } else {
                Log.e("MainActivity", "Bluetooth not enabled. The app cannot proceed.");
                Toast.makeText(this, "Bluetooth not enabled. The app cannot proceed.", Toast.LENGTH_SHORT).show();
            }
        }
        // Handle result from the device picker.
        else if (requestCode == REQUEST_CODE_DEVICE_PICKER && data != null) {
            BluetoothDevice device = data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                String macAddress = device.getAddress();
                Log.d("MainActivity", "Selected device MAC: " + macAddress);

                // Initialize Chaquopy and call the Python function.
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(this));
                }
                Python py = Python.getInstance();
                PyObject bitalinoModule = py.getModule("test");
                PyObject result = bitalinoModule.callAttr("start_bitalino_service", macAddress);
                Log.d("MainActivity", "Python result: " + result.toString());
            }
        }
    }
}
