package com.example.cinematch.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.cinematch.models.Movie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// DAO thao tác trực tiếp với 2 bảng trong DBHelper.
// Lưu ý: các hàm này chạy trên background thread (xem ThreadUtils), không gọi trực tiếp từ UI thread.
public class MovieCacheDAO {

    private final DBHelper dbHelper;

    public MovieCacheDAO(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    // ================= MOVIE CACHE =================

    // Lưu/update 1 phim vào cache khi user xem chi tiết phim đó
    public void cacheMovie(Movie movie) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_MOVIE_ID, movie.getId());
        values.put(DBHelper.COL_TITLE, movie.getTitle());
        values.put(DBHelper.COL_OVERVIEW, movie.getOverview());
        values.put(DBHelper.COL_POSTER_PATH, movie.getPosterPath());
        values.put(DBHelper.COL_BACKDROP_PATH, movie.getBackdropPath());
        values.put(DBHelper.COL_RELEASE_DATE, movie.getReleaseDate());
        values.put(DBHelper.COL_VOTE_AVERAGE, movie.getVoteAverage());
        values.put(DBHelper.COL_GENRE_IDS, genreIdsToString(movie.getGenreIds()));
        values.put(DBHelper.COL_CACHED_AT, System.currentTimeMillis());

        // insertWithOnConflict + REPLACE để tự update nếu movie_id đã tồn tại (PRIMARY KEY)
        db.insertWithOnConflict(DBHelper.TABLE_MOVIE_CACHE, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);

        trimCache(db, 30); // chỉ giữ tối đa 30 phim gần nhất để tránh cache phình to
    }

    // Lấy danh sách phim đã cache, mới nhất trước -> hiển thị khi mất mạng
    public List<Movie> getCachedMovies() {
        List<Movie> result = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(DBHelper.TABLE_MOVIE_CACHE, null, null, null,
                null, null, DBHelper.COL_CACHED_AT + " DESC");

        while (cursor.moveToNext()) {
            result.add(cursorToMovie(cursor));
        }
        cursor.close();
        return result;
    }

    // Lấy 1 phim cụ thể từ cache (dùng khi mất mạng mà user bấm vào phim đã xem trước đó)
    public Movie getCachedMovie(int movieId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TABLE_MOVIE_CACHE, null,
                DBHelper.COL_MOVIE_ID + "=?", new String[]{String.valueOf(movieId)},
                null, null, null);

        Movie movie = null;
        if (cursor.moveToFirst()) {
            movie = cursorToMovie(cursor);
        }
        cursor.close();
        return movie;
    }

    // Chỉ giữ lại N bản ghi mới nhất, xóa bớt bản ghi cũ
    private void trimCache(SQLiteDatabase db, int keepCount) {
        db.execSQL("DELETE FROM " + DBHelper.TABLE_MOVIE_CACHE +
                " WHERE " + DBHelper.COL_MOVIE_ID + " NOT IN (" +
                "SELECT " + DBHelper.COL_MOVIE_ID + " FROM " + DBHelper.TABLE_MOVIE_CACHE +
                " ORDER BY " + DBHelper.COL_CACHED_AT + " DESC LIMIT " + keepCount + ")");
    }

    private Movie cursorToMovie(Cursor cursor) {
        Movie movie = new Movie();
        movie.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_MOVIE_ID)));
        movie.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_TITLE)));
        movie.setOverview(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_OVERVIEW)));
        movie.setPosterPath(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POSTER_PATH)));
        movie.setBackdropPath(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_BACKDROP_PATH)));
        movie.setReleaseDate(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_RELEASE_DATE)));
        movie.setVoteAverage(cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COL_VOTE_AVERAGE)));
        movie.setGenreIds(stringToGenreIds(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_GENRE_IDS))));
        return movie;
    }

    private String genreIdsToString(List<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) return "";
        return genreIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Integer> stringToGenreIds(String raw) {
        List<Integer> ids = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return ids;
        for (String part : raw.split(",")) {
            try {
                ids.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) { }
        }
        return ids;
    }

    // ================= SEARCH HISTORY =================

    // Thêm 1 từ khóa tìm kiếm vào lịch sử
    public void addSearchHistory(String query) {
        if (query == null || query.trim().isEmpty()) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_QUERY, query.trim());
        values.put(DBHelper.COL_SEARCHED_AT, System.currentTimeMillis());
        db.insert(DBHelper.TABLE_SEARCH_HISTORY, null, values);
    }

    // Lấy N từ khóa tìm kiếm gần nhất -> gợi ý dropdown khi user gõ tìm kiếm
    public List<String> getRecentSearches(int limit) {
        List<String> result = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(true, DBHelper.TABLE_SEARCH_HISTORY,
                new String[]{DBHelper.COL_QUERY}, null, null, null, null,
                DBHelper.COL_SEARCHED_AT + " DESC", String.valueOf(limit));

        while (cursor.moveToNext()) {
            result.add(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_QUERY)));
        }
        cursor.close();
        return result;
    }

    public void clearSearchHistory() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DBHelper.TABLE_SEARCH_HISTORY, null, null);
    }
}
