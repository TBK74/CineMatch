package com.example.cinematch.network.response;

import com.google.gson.annotations.SerializedName;
import com.example.cinematch.models.Movie;

import java.util.List;

// Wrapper cho các endpoint trả về danh sách phim có phân trang:
// /movie/popular, /search/movie, /discover/movie...
// JSON dạng: { "page":1, "results":[...], "total_pages":.. , "total_results":.. }
public class MovieResponse {

    @SerializedName("page")
    private int page;

    @SerializedName("results")
    private List<Movie> results;

    @SerializedName("total_pages")
    private int totalPages;

    @SerializedName("total_results")
    private int totalResults;

    public int getPage() { return page; }
    public List<Movie> getResults() { return results; }
    public int getTotalPages() { return totalPages; }
    public int getTotalResults() { return totalResults; }
}
