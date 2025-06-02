// User.java
package com.example.sensorapplication;

public class User {
    public String name;
//    public String age;
//    public String gender;
    public String weight;
    public String height;
    public String stepGoal;

    public User(String name, String weight, String height, String stepGoal) {
        this.name = name;
//        this.age = age;
//        this.gender = gender;
        this.weight = weight;
        this.height = height;
        this.stepGoal = stepGoal;
    }

    public User() {

    }
}
