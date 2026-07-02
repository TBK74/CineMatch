package com.example.cinematch.network.response;

import com.google.gson.annotations.SerializedName;
import com.example.cinematch.models.Cast;

import java.util.List;

// Wrapper cho endpoint /movie/{id}/credits -> { "cast":[...], "crew":[...] }
// App chỉ cần cast nên không map crew.
public class CreditsResponse {

    @SerializedName("cast")
    private List<Cast> cast;

    public List<Cast> getCast() { return cast; }
}
