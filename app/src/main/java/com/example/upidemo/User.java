package com.example.upidemo;

public class User {
    public String userId;
    public String name;
    public String pin;
    public String phoneNumber;

    public User() {}
    public User(String userId, String name, String pin, String phoneNumber) {
        this.userId = userId;
        this.name = name;
        this.pin = pin;
        this.phoneNumber = phoneNumber;
    }
}
