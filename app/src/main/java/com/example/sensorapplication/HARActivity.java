package com.example.sensorapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
public class HARActivity extends AppCompatActivity implements SensorEventListener {

    private static final int TIME_STAMP = 100;
    private static final String TAG = "HARActivity";
    private static final String PREFS_NAME = "HAR_PREFS";
    private static final String KEY_SITTING = "sittingTime";
    private static final String KEY_JOGGING = "joggingTime";
    private static final String KEY_WALKING = "walkingTime";
    private float continuousSittingTime = 0f;
    private boolean reminderSent = false;
    private static final String CHANNEL_ID = "activity_channel";

    private List<Float> ax = new ArrayList<>(), ay = new ArrayList<>(), az = new ArrayList<>();
    private List<Float> gx = new ArrayList<>(), gy = new ArrayList<>(), gz = new ArrayList<>();
    private List<Float> lx = new ArrayList<>(), ly = new ArrayList<>(), lz = new ArrayList<>();
    private List<Float> ma = new ArrayList<>(), ml = new ArrayList<>(), mg = new ArrayList<>();

    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mGyroscope, mLinearAcceleration;
    private ActivityClassifier classifier;
    private float[] results;
    private PieChart pieChart;
    private BarChart barChart;
    private float sittingTime = 0f;
    private float joggingTime = 0f;
    private float walkingTime = 0f;



    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable predictionRunnable;
    private Timer midnightTimer;
    private Timer reminderTimer;


    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_har);
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        createNotificationChannel();

        // Send dummy notification to test
        sendSittingReminder(); // <---
        ImageView backButton = findViewById(R.id.backHarButton);
        backButton.setOnClickListener(v -> onBackPressed());

        loadSavedTimes();
        updateTextViews();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        try {
            classifier = new ActivityClassifier(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mLinearAcceleration, SensorManager.SENSOR_DELAY_UI);

        startPredictionInterval();
        scheduleMidnightReset();
    }

    private void loadSavedTimes() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sittingTime = prefs.getFloat(KEY_SITTING, 0f);
        joggingTime = prefs.getFloat(KEY_JOGGING, 0f);
        walkingTime = prefs.getFloat(KEY_WALKING, 0f);
    }

    private void saveTimes() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_SITTING, sittingTime);
        editor.putFloat(KEY_JOGGING, joggingTime);
        editor.putFloat(KEY_WALKING, walkingTime);
        editor.apply();
    }

    private void setupPieChart(float sitting, float jogging, float walking) {
        List<PieEntry> entries = new ArrayList<>();
        if (sitting > 0) entries.add(new PieEntry(sitting, "Sitting"));
        if (jogging > 0) entries.add(new PieEntry(jogging, "Jogging"));
        if (walking > 0) entries.add(new PieEntry(walking, "Walking"));

        PieDataSet dataSet = new PieDataSet(entries, "Activity Time (hrs)");
        dataSet.setColors(new int[]{
                Color.parseColor("#A7C7E7"),
                Color.parseColor("#CDB4DB"), // Light Lavender
                Color.parseColor("#F7CAC9")  // Pale Pink
        });


        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        // Set value formatter for 2 decimal points
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.2f", value);
            }
        });

        PieData pieData = new PieData(dataSet);

        Description description = new Description();
        description.setText("");
        pieChart.setDescription(description);
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(false);
        pieChart.invalidate();
    }
    private void setupBarChart(float sitting, float jogging, float walking) {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, sitting));
        entries.add(new BarEntry(1f, jogging));
        entries.add(new BarEntry(2f, walking));

        BarDataSet dataSet = new BarDataSet(entries, "Activity Time (hrs)");
        dataSet.setColors(new int[]{
                Color.parseColor("#A7C7E7"),
                Color.parseColor("#CDB4DB"), // Light Lavender
                Color.parseColor("#F7CAC9")
        });

        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(14f);

        // Format to 2 decimal points
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.2f", value);
            }
        });

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.getXAxis().setDrawLabels(true);
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                switch ((int) value) {
                    case 0: return "Sitting";
                    case 1: return "Jogging";
                    case 2: return "Walking";
                    default: return "";
                }
            }
        });

        Description description = new Description();
        description.setText("");
        barChart.setDescription(description);
        barChart.invalidate();
    }

    private void updateTextViews() {

        setupPieChart(sittingTime/3600, joggingTime/3600, walkingTime/3600);
        setupBarChart(sittingTime/3600, joggingTime/3600, walkingTime/3600);
//        sittingTextView.setText("🪑 Sitting: " + ((int) sittingTime / 60) + " min");
//        joggingTextView.setText("🏃‍♂️ Jogging: " + ((int) joggingTime / 60) + " min");
//        walkingTextView.setText("🚶‍♂️ Walking: " + ((int) walkingTime / 60) + " min");
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            ax.add(event.values[0]);
            ay.add(event.values[1]);
            az.add(event.values[2]);
        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gx.add(event.values[0]);
            gy.add(event.values[1]);
            gz.add(event.values[2]);
        } else {
            lx.add(event.values[0]);
            ly.add(event.values[1]);
            lz.add(event.values[2]);
        }
    }

    private void startPredictionInterval() {
        predictionRunnable = new Runnable() {
            @Override
            public void run() {
                predictAndUpdate();
                handler.postDelayed(this, 10000); // 10 seconds interval
            }
        };
        handler.postDelayed(predictionRunnable, 10000);
    }

    private void predictAndUpdate() {
        if (ax.size() >= TIME_STAMP && ay.size() >= TIME_STAMP && az.size() >= TIME_STAMP &&
                gx.size() >= TIME_STAMP && gy.size() >= TIME_STAMP && gz.size() >= TIME_STAMP &&
                lx.size() >= TIME_STAMP && ly.size() >= TIME_STAMP && lz.size() >= TIME_STAMP) {

            ma.clear(); ml.clear(); mg.clear();
            for (int i = 0; i < TIME_STAMP; i++) {
                ma.add((float) Math.sqrt(ax.get(i) * ax.get(i) + ay.get(i) * ay.get(i) + az.get(i) * az.get(i)));
                ml.add((float) Math.sqrt(lx.get(i) * lx.get(i) + ly.get(i) * ly.get(i) + lz.get(i) * lz.get(i)));
                mg.add((float) Math.sqrt(gx.get(i) * gx.get(i) + gy.get(i) * gy.get(i) + gz.get(i) * gz.get(i)));
            }

            List<Float> inputData = new ArrayList<>();
            inputData.addAll(ax.subList(0, TIME_STAMP));
            inputData.addAll(ay.subList(0, TIME_STAMP));
            inputData.addAll(az.subList(0, TIME_STAMP));
            inputData.addAll(lx.subList(0, TIME_STAMP));
            inputData.addAll(ly.subList(0, TIME_STAMP));
            inputData.addAll(lz.subList(0, TIME_STAMP));
            inputData.addAll(gx.subList(0, TIME_STAMP));
            inputData.addAll(gy.subList(0, TIME_STAMP));
            inputData.addAll(gz.subList(0, TIME_STAMP));
            inputData.addAll(ma.subList(0, TIME_STAMP));
            inputData.addAll(ml.subList(0, TIME_STAMP));
            inputData.addAll(mg.subList(0, TIME_STAMP));

            results = classifier.predictProbabilities(toFloatArray(inputData));
            Log.i(TAG, "Predicted Probabilities: " + Arrays.toString(results));

            // Aggregate into three categories
            float joggingProb = results[3] + results[4];
            float sittingProb = results[0] + results[1] + results[2] + results[5];
            float walkingProb = results[6];
            // Log the probabilities
            Log.d("ActivityProb", "Sitting Probability: " + sittingProb);
            Log.d("ActivityProb", "Jogging Probability: " + joggingProb);
            Log.d("ActivityProb", "Walking Probability: " + walkingProb);

            float maxProb = Math.max(sittingProb, Math.max(joggingProb, walkingProb));
            if (maxProb == sittingProb) {
                sittingTime += 10;
                continuousSittingTime += 10;
            } else {
                if (maxProb == joggingProb) {
                    joggingTime += 10;
                } else {
                    walkingTime += 10;
                }
                continuousSittingTime = 0f; // Reset if user is not sitting
            }

            updateTextViews();
            saveTimes();

            // Reminder logic
            if (continuousSittingTime >= 60 && !reminderSent) {
                sendSittingReminder();
                reminderSent = true;
            } else if (continuousSittingTime < 3600) {
                reminderSent = false; // Reset flag if user gets up
            }
            updateTextViews();

            // Clear buffers
            ax.clear(); ay.clear(); az.clear();
            gx.clear(); gy.clear(); gz.clear();
            lx.clear(); ly.clear(); lz.clear();
            ma.clear(); ml.clear(); mg.clear();

            // Save updated times
            saveTimes();
        }
    }

    private void sendSittingReminder() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, HARActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Time to Move")
                .setContentText("You are sitting more than an hour. Reminder to move!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify(1, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Activity Reminder";
            String description = "Channel for sitting reminder";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void scheduleMidnightReset() {
        midnightTimer = new Timer();
        Calendar now = Calendar.getInstance();
        Calendar midnight = (Calendar) now.clone();
        midnight.add(Calendar.DATE, 1);
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);

        long delay = midnight.getTimeInMillis() - now.getTimeInMillis();

        midnightTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    sittingTime = 0f;
                    joggingTime = 0f;
                    walkingTime = 0f;
                    updateTextViews();
                    saveTimes();
                });
            }
        }, delay, 24 * 60 * 60 * 1000); // Repeat every 24 hours
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i) != null ? list.get(i) : 0;
        }
        return array;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        handler.removeCallbacks(predictionRunnable);
        if (midnightTimer != null) midnightTimer.cancel();
        if (reminderTimer != null) reminderTimer.cancel();
    }
}
