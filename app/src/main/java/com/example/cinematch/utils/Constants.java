package com.example.cinematch.utils;

public class Constants {

    // TODO: thay bằng API key thật lấy từ https://www.themoviedb.org/settings/api
    public static final String TMDB_API_KEY = "b94b9b4d770543ec217a8008b59661fd";
    public static final String TMDB_BASE_URL = "https://api.themoviedb.org/3/";
    public static final String TMDB_LANGUAGE = "vi-VN"; // hoặc "en-US"

    // Firestore collection names
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_RATINGS = "ratings";
    public static final String COLLECTION_REVIEWS = "reviews";
    public static final String COLLECTION_WATCHLIST = "watchlist";
    public static final String COLLECTION_REPORTS = "reports";

    // SharedPreferences keys
    public static final String PREF_NAME = "cinematch_prefs";
    public static final String PREF_KEY_UID = "uid";
    public static final String PREF_KEY_ROLE = "role";
    public static final String PREF_KEY_DARK_MODE = "dark_mode";

    // SQLite
    public static final String DB_NAME = "cinematch_cache.db";
    public static final int DB_VERSION = 1;
}
