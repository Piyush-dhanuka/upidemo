package com.example.upidemo;

public class Review {
    public String reviewId;
    public String userId;
    public String userName;
    public float rating;
    public String comment;
    public long timestamp;

    public Review() {}

    public Review(String reviewId, String userId, String userName, float rating, String comment, long timestamp) {
        this.reviewId = reviewId;
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }
}
