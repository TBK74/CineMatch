package com.example.cinematch.models;

import com.google.gson.annotations.SerializedName;

// Model cho trailer, lấy từ endpoint /movie/{id}/videos của TMDB.
// "key" chính là YouTube video key -> nhúng qua WebView bằng URL youtube embed,
// KHÔNG cần gọi thêm YouTube Data API như yêu cầu đề bài.
public class Video {

    @SerializedName("id")
    private String id;

    @SerializedName("key")
    private String key;

    @SerializedName("name")
    private String name;

    @SerializedName("site")
    private String site; // thường là "YouTube"

    @SerializedName("type")
    private String type; // "Trailer", "Teaser"...

    public Video() { }

    public String getId() { return id; }
    public String getKey() { return key; }
    public String getName() { return name; }
    public String getSite() { return site; }
    public String getType() { return type; }

    public boolean isYoutubeTrailer() {
        return "YouTube".equalsIgnoreCase(site) && "Trailer".equalsIgnoreCase(type);
    }

    // URL để nhúng vào WebView ở màn Movie Detail
    public String getEmbedUrl() {
        return "https://www.youtube.com/embed/" + key;
    }
}
