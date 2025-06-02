package com.example.sensorapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepCountActivity extends Activity implements SensorEventListener, LocationListener {
    private static final String TAG = "MainActivity";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private LocationManager locationManager;

    private TextView stepCountTextView;
    private CircularProgressIndicator stepProgressIndicator;

    private long lastStepTime = 0;
    private static final long MIN_STEP_INTERVAL = 300; // milliseconds
    private int stepCount = 0;

    private static final int SMOOTHING_WINDOW_SIZE = 20;
    private float[][] mAccelValueHistory = new float[3][SMOOTHING_WINDOW_SIZE];
    private float[] mRunningAccelTotal = new float[3];
    private float[] mCurAccelAvg = new float[3];
    private int mCurReadIndex = 0;

    private double lastMag = 0d;
    private double avgMag = 0d;
    private double netMag = 0d;

    private double lastNetMag = 0;
    private boolean wasIncreasing = false;
    private static final double STEP_THRESHOLD = 1.0;

    private float[] lastGyroValues = new float[3];
    private static final float ROTATION_THRESHOLD = 1.5f; // radians/sec

    private Location lastLocation = null;
    private static final float MIN_MOVEMENT_DISTANCE = 3.0f;
    private static final long GPS_VALIDATION_WINDOW = 5000;
    private long lastGpsCheckTime = 0;
    private boolean gpsMovementDetected = false;

    private static final int REQUEST_LOCATION_PERMISSION = 100;

    private TextView tvDistance ,tvStepGoalLabel;
    private TextView tvCalories;
    private TextView tvTime;
    private static final String PREFS_NAME = "StepPrefs";
    private static final String PREF_STEP_COUNT = "stepCount";
    private static final String PREF_DATE = "stepDate";
    private static final String PREF_LAST_DATE="last_date";
    private static final double STEP_LENGTH_METERS = 0.78;
    private static final double CALORIES_PER_STEP = 0.04;

    private int lastStepCount = 0;
    private User user;
    private long lastStepTimestamp = 0L;
    private long totalWalkingTimeMillis = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_count);

        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        tvStepGoalLabel = findViewById(R.id.tvStepGoalLabel);
        stepCountTextView = findViewById(R.id.tvSteps);
        stepProgressIndicator = findViewById(R.id.stepProgress);
        tvDistance = findViewById(R.id.tvDistance);
        tvCalories = findViewById(R.id.tvCalories);
//        tvTime = findViewById(R.id.tvTime);
        DBHelper dbHelper = new DBHelper(this);
        user = dbHelper.getUserDetails();

        TextView tvDate = findViewById(R.id.tvDate);
        String currentDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
        tvDate.setText(currentDate);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedDate = prefs.getString(PREF_DATE, "");
//        int targetSteps = prefs.getInt("target_steps", 10000);
//        stepProgressIndicator.setMax(targetSteps);

        if (savedDate.equals(currentDate)) {
            stepCount = prefs.getInt(PREF_STEP_COUNT, 0); // Restore step count
        } else {
            stepCount = 0; // New day, reset
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_DATE, currentDate);
            editor.putInt(PREF_STEP_COUNT, stepCount);
            editor.apply();}
        updateStepCountUI();

        updateStepStats(stepCount);

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Accelerometer registered");
        }

        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Gyroscope registered");
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
        int targetSteps = 10000; // default value

        if (user.stepGoal != null) {
            try {
                // Extract numeric part using regex
                String digitsOnly = user.stepGoal.replaceAll("[^0-9]", "");
                targetSteps = Integer.parseInt(digitsOnly);
            } catch (NumberFormatException e) {
                // Log the error if needed, and keep default value
                e.printStackTrace();
            }
        }

        updateTargetSteps(targetSteps);

    }
    private void updateTargetSteps(int newTarget) {
        tvStepGoalLabel.setText("Target:" + newTarget);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("target_steps", newTarget);
        editor.apply();

        // Update the UI
        stepProgressIndicator.setMax(newTarget);
    }
    private void requestLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission denied", e);
        }
    }

    private void updateStepCountUI() {
        runOnUiThread(() -> stepCountTextView.setText(String.valueOf(stepCount)));
        stepProgressIndicator.setProgress(stepCount);
    }
    private float convertHeightToCm(String heightStr) {
        try {
            // Expecting format like: 5'8"
            String[] parts = heightStr.split("'");
            int feet = Integer.parseInt(parts[0].trim());
            int inches = Integer.parseInt(parts[1].replace("\"", "").trim());
            float totalInches = feet * 12 + inches;
            return totalInches * 2.54f; // 1 inch = 2.54 cm
        } catch (Exception e) {
            e.printStackTrace();
            return 160.0f; // fallback default height
        }
    }
    private float convertWeightToKg(String weightStr) {
        try {
            return Float.parseFloat(weightStr.replace(" kg", "").trim());
        } catch (Exception e) {
            e.printStackTrace();
            return 70.0f; // fallback
        }
    }

    private void updateStepStats(int newStepCount) {
        long currentTime = System.currentTimeMillis();

        if (newStepCount > stepCount) {
            if (lastStepTimestamp != 0) {
                totalWalkingTimeMillis += (currentTime - lastStepTimestamp);
            }
            lastStepTimestamp = currentTime;
        }

        stepCount = newStepCount;


//        double distance = (stepCount * STEP_LENGTH_METERS) / 1000.0;
//        double calories = stepCount * CALORIES_PER_STEP;


        if (user != null) {
            float height = convertHeightToCm(user.height);// in cm
            float weight = convertWeightToKg(user.weight);
            // Convert height to meters if needed
            float strideLength = height * 0.414f; // in cm

            double distance = stepCount * strideLength; // in cm
            double distanceKm = distance / 100000f; // cm to km

            double calories = stepCount * 0.04f * (weight / 70f) * (height / 160f);
            Log.d(TAG, "calories" + calories);
            Log.d(TAG, "distance" + distance);
            runOnUiThread(() -> {
                stepProgressIndicator.setProgress(stepCount);
                tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", distanceKm));
                tvCalories.setText(String.format(Locale.getDefault(), "%.0f cal", calories));
//            tvTime.setText(String.format(Locale.getDefault(), "%dh %02dm", minutes / 60, minutes % 60));
            });
        }else{

            double distance = (stepCount * STEP_LENGTH_METERS) / 1000.0;
            double calories = stepCount * CALORIES_PER_STEP;
            runOnUiThread(() -> {
                stepProgressIndicator.setProgress(stepCount);
                tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", distance));
                tvCalories.setText(String.format(Locale.getDefault(), "%.0f cal", calories));
//            tvTime.setText(String.format(Locale.getDefault(), "%dh %02dm", minutes / 60, minutes % 60));
            });

        }

//        long totalSeconds = totalWalkingTimeMillis / 1000;
//        long minutes = totalSeconds / 60;

    }

    private boolean isRotating() {
        float magnitude = (float) Math.sqrt(
                lastGyroValues[0] * lastGyroValues[0] +
                        lastGyroValues[1] * lastGyroValues[1] +
                        lastGyroValues[2] * lastGyroValues[2]
        );
        return magnitude > ROTATION_THRESHOLD;
    }

    private void detectStep(double netMag) {
        long currentTime = System.currentTimeMillis();

        if (isRotating()) {
            Log.d(TAG, "Skipping step detection: phone is rotating");
            return;
        }

        // Check if enough GPS movement was detected recently
        if (currentTime - lastGpsCheckTime > GPS_VALIDATION_WINDOW) {
            gpsMovementDetected = false; // Reset gps movement flag every window
        }

        if (netMag > STEP_THRESHOLD && !wasIncreasing && lastNetMag < netMag) {
            // Check if GPS movement detected or GPS unavailable (null)
            if (gpsMovementDetected || lastLocation == null) {
                if (currentTime - lastStepTime > MIN_STEP_INTERVAL) {
                    stepCount++;
                    lastStepTime = currentTime;

                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(PREF_STEP_COUNT, stepCount);
                    editor.putString(PREF_LAST_DATE, new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
                    editor.apply();


                    // Update walking time
                    if (lastStepTimestamp != 0) {
                        totalWalkingTimeMillis += (currentTime - lastStepTimestamp);
                    }
                    lastStepTimestamp = currentTime;

                    updateStepCountUI();
                    updateStepStats(stepCount);
                    Log.d(TAG, "Step detected! Total steps: " + stepCount);

                }
            } else {
                Log.d(TAG, "Step ignored due to no GPS movement");
            }
            wasIncreasing = true;
        } else if (netMag < lastNetMag) {
            wasIncreasing = false;
        }

        lastNetMag=netMag;}

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0], y = event.values[1], z = event.values[2];

            lastMag = Math.sqrt(x * x + y * y + z * z);

            for (int i = 0; i < 3; i++) {
                mRunningAccelTotal[i] -= mAccelValueHistory[i][mCurReadIndex];
                mAccelValueHistory[i][mCurReadIndex] = (i == 0) ? x : (i == 1) ? y : z;
                mRunningAccelTotal[i] += mAccelValueHistory[i][mCurReadIndex];
                mCurAccelAvg[i] = mRunningAccelTotal[i] / SMOOTHING_WINDOW_SIZE;
            }

            mCurReadIndex = (mCurReadIndex + 1) % SMOOTHING_WINDOW_SIZE;

            avgMag = Math.sqrt(
                    mCurAccelAvg[0] * mCurAccelAvg[0] +
                            mCurAccelAvg[1] * mCurAccelAvg[1] +
                            mCurAccelAvg[2] * mCurAccelAvg[2]
            );

            netMag = lastMag - avgMag;
            detectStep(netMag);
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            lastGyroValues = event.values.clone();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (lastLocation != null) {
            float distance = location.distanceTo(lastLocation);
            if (distance > MIN_MOVEMENT_DISTANCE) {
                gpsMovementDetected = true;
                lastGpsCheckTime = System.currentTimeMillis();
                lastLocation = location;
            }
        } else {
            lastLocation = location;
        }
    }

    private void saveStepData() {
        SharedPreferences.Editor editor = getSharedPreferences("StepPrefs", MODE_PRIVATE).edit();
        editor.putInt("stepCount", stepCount);
        editor.putLong("totalWalkingTime", totalWalkingTimeMillis);
        editor.putLong("lastStepTimestamp", lastStepTimestamp);
        editor.apply();
    }

    private void loadStepData() {
        SharedPreferences prefs = getSharedPreferences("StepPrefs", MODE_PRIVATE);
        stepCount = prefs.getInt("stepCount", 0);
        totalWalkingTimeMillis = prefs.getLong("totalWalkingTime", 0);
        lastStepTimestamp = prefs.getLong("lastStepTimestamp", 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);
        saveStepData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveStepData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        if (gyroscope != null)
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates();
        }

        updateStepCountUI();
        updateStepStats(stepCount);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onProviderEnabled(@NonNull String provider) {}
    @Override
    public void onProviderDisabled(@NonNull String provider) {}
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
