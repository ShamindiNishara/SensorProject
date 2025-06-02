package com.example.sensorapplication;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {
    private DBHelper dbHelper; // Database helper instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this); // Initialize DBHelper

        // Check if a registered user exists
        if (isUserRegistered()) {
            // If user is registered, navigate directly to Dashboard
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish(); // Close MainActivity so the user can't go back
        } else {
            // If no user is registered, show Profile Registration Activity
            Button nextButton = findViewById(R.id.nextButton);
            nextButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ProfileRegActivity.class);
                startActivity(intent);
            });
        }
    }

    // Check if a user is already registered
    private boolean isUserRegistered() {
        String userName = dbHelper.getUser(); // Get the user from the database
        return userName != null && !userName.isEmpty(); // If userName is not null or empty, user is registered
    }
}
