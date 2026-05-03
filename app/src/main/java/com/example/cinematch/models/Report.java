package com.example.cinematch.models;

import com.google.firebase.firestore.Exclude;

// Document trong collection "reports". Moderator Dashboard sẽ query collection này.
public class Report {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_RESOLVED = "resolved";

    @Exclude
    private String reportId;

    private String reviewId;
    private String reportedBy;
    private String reason;
    private long createdAt;
    private String status;

    public Report() { }

    public Report(String reviewId, String reportedBy, String reason) {
        this.reviewId = reviewId;
        this.reportedBy = reportedBy;
        this.reason = reason;
        this.createdAt = System.currentTimeMillis();
        this.status = STATUS_PENDING;
    }

    @Exclude
    public String getReportId() { return reportId; }
    @Exclude
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getReviewId() { return reviewId; }
    public String getReportedBy() { return reportedBy; }
    public String getReason() { return reason; }
    public long getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }

    public void setReviewId(String reviewId) { this.reviewId = reviewId; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }
    public void setReason(String reason) { this.reason = reason; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setStatus(String status) { this.status = status; }
}
