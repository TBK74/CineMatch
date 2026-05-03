package com.example.cinematch.models;

// Document trong collection "watchlist". Id = userId + "_" + movieId.
public class WatchlistItem {

    private String userId;
    private int movieId;
    private String movieTitle;
    private String posterPath;
    private long addedAt;

    public WatchlistItem() { }

    public WatchlistItem(String userId, int movieId, String movieTitle, String posterPath) {
        this.userId = userId;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.posterPath = posterPath;
        this.addedAt = System.currentTimeMillis();
    }

    public String getUserId() { return userId; }
    public int getMovieId() { return movieId; }
    public String getMovieTitle() { return movieTitle; }
    public String getPosterPath() { return posterPath; }
    public long getAddedAt() { return addedAt; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public void setAddedAt(long addedAt) { this.addedAt = addedAt; }
}
