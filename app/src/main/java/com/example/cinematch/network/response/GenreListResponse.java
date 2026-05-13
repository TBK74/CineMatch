package com.example.cinematch.network.response;

import com.google.gson.annotations.SerializedName;
import com.example.cinematch.models.Genre;

import java.util.List;

// Wrapper cho endpoint /genre/movie/list -> { "genres": [ {id, name}, ... ] }
public class GenreListResponse {

    @SerializedName("genres")
    private List<Genre> genres;

    public List<Genre> getGenres() { return genres; }
}
