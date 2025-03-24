package com.example.sensorapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";
    private SensorManager sensorManager;
    Sensor accelerometer;
    private TextView stepCountTextView;
    private TextView distanceView;
    private TextView timeView;
    private Button pauseButton;

    private Integer stepCount = 0 ;
    private ProgressBar progressBar;
    private boolean isPaused = false;
    private long timePaused = 0;
    private float stepLengthInMeters = 0.762f;     //step length for a adult
    private long startTime;
    private int stepCountTarget = 5000;
    private TextView stepCountTargetTextView;
    private double magnitudePrevious = 0;
    private static final float STEP_THRESHOLD = 1.5f; // Acceleration threshold for a step
    private static final int STEP_DELAY_MS = 300;
    private long lastStepTime = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        stepCountTextView = findViewById(R.id.stepCountView);
        distanceView = findViewById(R.id.distanceView);
        timeView  = findViewById(R.id.timeView);
        pauseButton = findViewById(R.id.pauseBtn);
        stepCountTargetTextView = findViewById(R.id.stepCounterTargetTextView);

        startTime = System.currentTimeMillis();

        //get the permission to use sensor service
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //use acceloromter
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //monitor accelorometer without delay
        sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        //sensor delay normal is the sample rate which called the onsensorchanged method (5 times per second)
        Log.d(TAG, "onCrate: Registered accelerometer listener");



    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//        Log.d(TAG, "OnsensorChanged: X: " + event.values[0] + "Y : " + event.values[1] + "Z: " + event.values[2]);

        float x = event.values[0]; // X-axis acceleration
        float y = event.values[1]; // Y-axis acceleration
        float z = event.values[2]; // Z-axis acceleration

        // Calculate magnitude of acceleration vector (removing gravity effect)
        float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - 9.8f;

        // Check if acceleration crosses the step threshold
        if (acceleration > STEP_THRESHOLD) {
            long currentTime = System.currentTimeMillis();

            // Ensure there is a delay between steps to prevent double counting
            if ((currentTime - lastStepTime) > STEP_DELAY_MS) {
                stepCount++; // Increment step count
                lastStepTime = currentTime; // Update last step time

                // Display step count in UI
                stepCountTextView.setText("Steps: " + stepCount);

                // Log step count
                Log.d(TAG, "Step detected! Total Steps: " + stepCount);
            }
//        textView.setText("OnsensorChanged: X: " + event.values[0] + " Y : " + event.values[1] + " Z: " + event.values[2]);


    }}

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    protected void onStop(){
        super.onStop();
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.putInt("stepCount" , stepCount);
        editor.apply();
    }

    protected void onResume(){
        super.onResume();
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        stepCount = sharedPreferences.getInt("stepCount",0);
        
    }
}
