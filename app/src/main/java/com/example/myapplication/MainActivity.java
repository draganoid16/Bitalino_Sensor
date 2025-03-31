package com.example.myapplication;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Start Chaquopy Python if not already started
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        // Find the custom semi-circular progress view
        SemiCircularProgressBar semiCircularProgress = findViewById(R.id.semiCircularProgress);

        // Animate the progress from 0 to 52 (out of 100) over 2 seconds
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(semiCircularProgress, "progress", 0, 52);
        progressAnimator.setDuration(2000); // animation duration in milliseconds
        progressAnimator.start();

    }
}
