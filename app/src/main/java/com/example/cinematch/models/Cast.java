package com.example.cinematch.models;

import com.google.gson.annotations.SerializedName;

// Model cho 1 diễn viên, dùng ở màn Movie Detail (endpoint /movie/{id}/credits)
public class Cast {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("character")
    private String character;

    @SerializedName("profile_path")
    private String profilePath;

    public Cast() { }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCharacter() { return character; }
    public String getProfilePath() { return profilePath; }

    public String getFullProfileUrl() {
        if (profilePath == null) return null;
        return "https://image.tmdb.org/t/p/w185" + profilePath;
    }
}
