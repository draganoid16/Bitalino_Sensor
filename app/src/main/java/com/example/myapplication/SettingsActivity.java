package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the settings layout (assume it's named activity_settings.xml)
        setContentView(R.layout.activity_settings);

        // Back button logic: finishes the activity when the back icon is clicked.
        ImageView backIcon = findViewById(R.id.backIcon);
        backIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // Find the Spinner used for selecting the sensor type.
        final Spinner sensorSpinner = findViewById(R.id.sensorTypeSpinner);
        // Optionally, you can set up the Spinner adapter here if needed.

        // Start Recording button: when clicked, show a Toast with the selected sensor.
        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String selectedSensor = sensorSpinner.getSelectedItem().toString();
                Toast.makeText(SettingsActivity.this,
                        "Started recording data using " + selectedSensor,
                        Toast.LENGTH_SHORT).show();
                // Add your data recording logic here.
            }
        });
    }
}
