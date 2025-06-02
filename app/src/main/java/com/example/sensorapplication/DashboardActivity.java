package com.example.sensorapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class DashboardActivity extends AppCompatActivity {

    private TextView txtDashboard;
    private DBHelper dbHelper;
    // if you want to load dynamic image
    private CardView cardActivityLog;
    private CardView cardStepCount;
    private ImageView profileView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard); // Ensure your XML file is named activity_dashboard.xml
        cardStepCount = findViewById(R.id.card_step_count);
        cardActivityLog = findViewById(R.id.card_activity_log);
        txtDashboard = findViewById(R.id.txtDashboard);
        profileView = findViewById(R.id.profile);
        dbHelper = new DBHelper(this);

        displayUserName();
        // Set click listener on the CardView
        cardActivityLog.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, HARActivity.class);
            startActivity(intent);
        });
        cardStepCount.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, StepCountActivity.class);
            startActivity(intent);
        });
        profileView.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ProfileRegActivity.class);
            intent.putExtra(ProfileRegActivity.EXTRA_IS_UPDATE, true); // Launch in update mode
            startActivity(intent);
        });
    }
        private void displayUserName() {
            String user = dbHelper.getUser(); // Assuming only one user in your app
            if (user != null) {
                txtDashboard.setText("Hello, " + user + "!");
            } else {
                txtDashboard.setText("Hello!");
            }
        }

}