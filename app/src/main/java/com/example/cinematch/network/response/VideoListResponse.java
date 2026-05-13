package com.example.cinematch.network.response;

import com.google.gson.annotations.SerializedName;
import com.example.cinematch.models.Video;

import java.util.List;

// Wrapper cho endpoint /movie/{id}/videos -> { "id":.., "results":[...] }
public class VideoListResponse {

    @SerializedName("results")
    private List<Video> results;

    public List<Video> getResults() { return results; }
}
