package com.example.cinematch.models;

import com.google.firebase.firestore.Exclude;

// Document trong collection "reviews".
public class Review {

    public static final String STATUS_VISIBLE = "visible";
    public static final String STATUS_HIDDEN = "hidden";

    @Exclude
    private String reviewId; // set thủ công sau khi query, không lưu vào Firestore

    private String userId;
    private String userName;
    private int movieId;
    private String content;
    private long createdAt;
    private String status;
    private int reportCount;

    public Review() { }

    public Review(String userId, String userName, int movieId, String content) {
        this.userId = userId;
        this.userName = userName;
        this.movieId = movieId;
        this.content = content;
        this.createdAt = System.currentTimeMillis();
        this.status = STATUS_VISIBLE;
        this.reportCount = 0;
    }

    @Exclude
    public String getReviewId() { return reviewId; }
    @Exclude
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public int getMovieId() { return movieId; }
    public String getContent() { return content; }
    public long getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public int getReportCount() { return reportCount; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setMovieId(int movieId) { this.movieId = movieId; }
    public void setContent(String content) { this.content = content; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setStatus(String status) { this.status = status; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }
}
