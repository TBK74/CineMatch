package com.example.cinematch.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.cinematch.utils.Constants;

// SQLiteOpenHelper quản lý 2 bảng:
// 1) movie_cache    -> cache phim đã xem gần đây để xem offline khi mất mạng
// 2) search_history -> lưu lịch sử tìm kiếm của user
public class DBHelper extends SQLiteOpenHelper {

    // --- Bảng movie_cache ---
    public static final String TABLE_MOVIE_CACHE = "movie_cache";
    public static final String COL_MOVIE_ID = "movie_id";       // PRIMARY KEY, trùng id TMDB
    public static final String COL_TITLE = "title";
    public static final String COL_OVERVIEW = "overview";
    public static final String COL_POSTER_PATH = "poster_path";
    public static final String COL_BACKDROP_PATH = "backdrop_path";
    public static final String COL_RELEASE_DATE = "release_date";
    public static final String COL_VOTE_AVERAGE = "vote_average";
    public static final String COL_GENRE_IDS = "genre_ids";     // lưu dạng "28,12,16" (comma-separated)
    public static final String COL_CACHED_AT = "cached_at";

    // --- Bảng search_history ---
    public static final String TABLE_SEARCH_HISTORY = "search_history";
    public static final String COL_HISTORY_ID = "_id";
    public static final String COL_QUERY = "query_text";
    public static final String COL_SEARCHED_AT = "searched_at";

    private static DBHelper instance;

    public static synchronized DBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DBHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DBHelper(Context context) {
        super(context, Constants.DB_NAME, null, Constants.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_MOVIE_CACHE + " (" +
                COL_MOVIE_ID + " INTEGER PRIMARY KEY, " +
                COL_TITLE + " TEXT, " +
                COL_OVERVIEW + " TEXT, " +
                COL_POSTER_PATH + " TEXT, " +
                COL_BACKDROP_PATH + " TEXT, " +
                COL_RELEASE_DATE + " TEXT, " +
                COL_VOTE_AVERAGE + " REAL, " +
                COL_GENRE_IDS + " TEXT, " +
                COL_CACHED_AT + " INTEGER" +
                ")");

        db.execSQL("CREATE TABLE " + TABLE_SEARCH_HISTORY + " (" +
                COL_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_QUERY + " TEXT, " +
                COL_SEARCHED_AT + " INTEGER" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Đồ án chỉ có 1 version, upgrade đơn giản là xóa tạo lại
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MOVIE_CACHE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEARCH_HISTORY);
        onCreate(db);
    }
}
