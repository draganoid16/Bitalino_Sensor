package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity extends AppCompatActivity {

    private Handler handler = new Handler();
    private TextView clockTextView;
    private Python py;
    private PyObject clockModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enable edge-to-edge (optional, as in your original layout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Start Chaquopy Python if not already started
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        // Get reference to the TextView that will display the clock time
        clockTextView = findViewById(R.id.textview);

        // Initialize Python and load the clock module (e.g., clock_module.py)
        py = Python.getInstance();
        clockModule = py.getModule("clock_module");

        // Call the Python function 'service_started' to perform any setup tasks
        clockModule.callAttr("service_started");

        // Start a periodic update of the clock using a Handler
        handler.post(updateClockRunnable);
    }

    // Runnable that calls the Python function 'update_clock' every second
    private Runnable updateClockRunnable = new Runnable() {
        @Override
        public void run() {
            // Call Python function update_clock() and update the TextView with the returned time
            PyObject result = clockModule.callAttr("update_clock");
            clockTextView.setText(result.toString());
            // Schedule the next update after 1000 milliseconds (1 second)
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onDestroy() {
        // Remove the callback when the activity is destroyed to prevent memory leaks
        handler.removeCallbacks(updateClockRunnable);
        super.onDestroy();
    }
}
