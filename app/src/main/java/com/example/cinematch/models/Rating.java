package com.example.cinematch.models;

import java.util.List;

// Document trong collection "ratings". Id = userId + "_" + movieId (xem FirestoreManager).
// Lưu kèm genreIds để RecommendationEngine tính genre weight mà không cần gọi lại TMDB.
public class Rating {

    private String userId;
    private int movieId;
    private List<Integer> genreIds;
    private double score;      // thang 1-10
    private long ratedAt;

    public Rating() { }

    public Rating(String userId, int movieId, List<Integer> genreIds, double score) {
        this.userId = userId;
        this.movieId = movieId;
        this.genreIds = genreIds;
        this.score = score;
        this.ratedAt = System.currentTimeMillis();
    }

    public String getUserId() { return userId; }
    public int getMovieId() { return movieId; }
    public List<Integer> getGenreIds() { return genreIds; }
    public double getScore() { return score; }
    public long getRatedAt() { return ratedAt; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }
    public void setGenreIds(List<Integer> genreIds) { this.genreIds = genreIds; }
    public void setScore(double score) { this.score = score; }
    public void setRatedAt(long ratedAt) { this.ratedAt = ratedAt; }
}
