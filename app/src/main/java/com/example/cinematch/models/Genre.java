package com.example.cinematch.models;

import com.google.gson.annotations.SerializedName;

// Model tương ứng 1 thể loại phim từ TMDB (vd: {"id":28,"name":"Action"})
public class Genre {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    public Genre() { } // bắt buộc có constructor rỗng cho Gson

    public Genre(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
}
