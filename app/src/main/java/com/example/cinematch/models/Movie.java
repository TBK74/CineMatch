package com.example.cinematch.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

// Model chính đại diện 1 bộ phim, map trực tiếp field JSON từ TMDB (/movie/{id}, /discover/movie...)
// Serializable để có thể truyền qua Intent giữa các Activity
public class Movie implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("overview")
    private String overview;

    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("backdrop_path")
    private String backdropPath;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("vote_average")
    private double voteAverage;

    // Khi gọi /movie/popular hay /discover, TMDB trả về genre_ids (mảng số)
    @SerializedName("genre_ids")
    private List<Integer> genreIds;

    // Khi gọi /movie/{id} (chi tiết), TMDB trả về genres (mảng object đầy đủ)
    @SerializedName("genres")
    private List<Genre> genres;

    // Field tự tính thêm ở tầng app, KHÔNG có trong JSON gốc của TMDB.
    // Dùng để sort danh sách gợi ý cá nhân hóa -> xem RecommendationEngine
    private transient double genreMatchScore;

    public Movie() { }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getOverview() { return overview; }
    public String getPosterPath() { return posterPath; }
    public String getBackdropPath() { return backdropPath; }
    public String getReleaseDate() { return releaseDate; }
    public double getVoteAverage() { return voteAverage; }
    public List<Integer> getGenreIds() { return genreIds; }
    public List<Genre> getGenres() { return genres; }
    public double getGenreMatchScore() { return genreMatchScore; }

    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setOverview(String overview) { this.overview = overview; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public void setBackdropPath(String backdropPath) { this.backdropPath = backdropPath; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public void setVoteAverage(double voteAverage) { this.voteAverage = voteAverage; }
    public void setGenreIds(List<Integer> genreIds) { this.genreIds = genreIds; }
    public void setGenres(List<Genre> genres) { this.genres = genres; }
    public void setGenreMatchScore(double genreMatchScore) { this.genreMatchScore = genreMatchScore; }

    // Ghép link poster đầy đủ để load bằng Glide/Picasso hoặc cache Internal Storage
    public String getFullPosterUrl() {
        if (posterPath == null) return null;
        return "https://image.tmdb.org/t/p/w500" + posterPath;
    }

    public String getFullBackdropUrl() {
        if (backdropPath == null) return null;
        return "https://image.tmdb.org/t/p/w780" + backdropPath;
    }
}
