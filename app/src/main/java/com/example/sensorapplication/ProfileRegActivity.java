package com.example.sensorapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class ProfileRegActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    public static final String EXTRA_IS_UPDATE = "is_update";


    private TextInputEditText nameEditText;
    private AutoCompleteTextView ageDropdown, genderDropdown, weightDropdown, heightDropdown, stepGoalDropdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_reg);

        dbHelper = new DBHelper(this);

        nameEditText = findViewById(R.id.nameEditText);
        weightDropdown = findViewById(R.id.weightDropdown);
        heightDropdown = findViewById(R.id.heightDropdown);
        stepGoalDropdown = findViewById(R.id.stepGoalDropdown);

        setupDropdowns();

        // Check if in update mode
        boolean isUpdate = getIntent().getBooleanExtra(EXTRA_IS_UPDATE, false);
        if (isUpdate) {
            populateUserData(); // Pre-fill the form
        }

        AppCompatButton continueButton = findViewById(R.id.continueButton);
        continueButton.setText(isUpdate ? "Update Profile" : "Continue"); // Button label change

        continueButton.setOnClickListener(view -> {
            saveUserInfo();
            if (isUpdate) {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                finish(); // Go back to Dashboard
            } else {
                startActivity(new Intent(ProfileRegActivity.this, DashboardActivity.class));
            }
        });
    }

    private void populateUserData() {
        User user = dbHelper.getUserDetails();  // You should already have this method

        if (user != null) {
            nameEditText.setText(user.name);
            weightDropdown.setText(user.weight, false);
            heightDropdown.setText(user.height, false);
            stepGoalDropdown.setText(user.stepGoal, false);
        }
    }

    private void setupDropdowns() {
        stepGoalDropdown.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{ "6000 steps", "8000 steps", "10000 steps", "12000 steps"}));

        String[] ages = new String[43];
        for (int i = 0; i < ages.length; i++) ages[i] = String.valueOf(18 + i);
//        ageDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, ages));
//
//        genderDropdown.setAdapter(new ArrayAdapter<>(this,
//                android.R.layout.simple_dropdown_item_1line,
//                new String[]{"Male", "Female", "Other"}));

        String[] weights = new String[51];
        for (int i = 0; i < weights.length; i++) weights[i] = (50 + i) + " kg";
        weightDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, weights));

        String[] heights = new String[37];
        for (int i = 0; i < 37; i++) {
            int feet = (48 + i) / 12;
            int inches = (48 + i) % 12;
            heights[i] = feet + "'" + inches + "\"";
        }
        heightDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, heights));
    }

    private void saveUserInfo() {
        String name = nameEditText.getText().toString().trim();
//        String age = ageDropdown.getText().toString().trim();
//        String gender = genderDropdown.getText().toString().trim();
        String weight = weightDropdown.getText().toString().trim();
        String height = heightDropdown.getText().toString().trim();
        String stepGoal = stepGoalDropdown.getText().toString().trim();

        if (name.isEmpty() ||
                weight.isEmpty() || height.isEmpty() || stepGoal.isEmpty()) {
            Toast.makeText(this, "Please complete all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User(name, weight, height, stepGoal);
        dbHelper.saveUser(user);
        Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();

        // Navigate to Dashboard
        Intent intent = new Intent(ProfileRegActivity.this, DashboardActivity.class);
        startActivity(intent);
//        finish();
    }

}
