// DBHelper.java
package com.example.sensorapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "user.db"; // Persistent storage
    private static final int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE user (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "weight TEXT," +
                "height TEXT," +
                "stepGoal TEXT" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS user");
        onCreate(db);
    }

    public void saveUser(User user) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("user", null, null); // Allow only one user
        ContentValues values = new ContentValues();
        values.put("name", user.name);
        values.put("weight", user.weight);
        values.put("height", user.height);
        values.put("stepGoal", user.stepGoal);
        db.insert("user", null, values);
    }
    public String getUser() {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {"name"}; // We only need the name column
        Cursor cursor = db.query("user", columns, null, null, null, null, null);

        String userName = null;
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex("name");
            if (nameIndex != -1) {
                userName = cursor.getString(nameIndex);
            }
            cursor.close();
        }
        return userName;
    }
    public User getUserDetails() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query("user", null, null, null, null, null, null);

        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = new User();
            user.name = cursor.getString(cursor.getColumnIndex("name"));
            user.weight = cursor.getString(cursor.getColumnIndex("weight"));
            user.height = cursor.getString(cursor.getColumnIndex("height"));
            user.stepGoal = cursor.getString(cursor.getColumnIndex("stepGoal"));
            cursor.close();
        }
        return user;
    }



}
