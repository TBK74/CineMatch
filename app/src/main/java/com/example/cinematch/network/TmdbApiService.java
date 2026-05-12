package com.example.cinematch.network;

import com.example.cinematch.network.response.CreditsResponse;
import com.example.cinematch.network.response.GenreListResponse;
import com.example.cinematch.network.response.MovieResponse;
import com.example.cinematch.network.response.VideoListResponse;
import com.example.cinematch.models.Movie;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

// Interface định nghĩa toàn bộ endpoint TMDB mà app dùng.
// Retrofit sẽ tự sinh implementation từ interface này (proxy pattern).
public interface TmdbApiService {

    // Danh sách phim đang hot -> dùng cho banner ở Home
    @GET("movie/popular")
    Call<MovieResponse> getPopularMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("page") int page
    );

    // Chi tiết 1 phim -> Movie Detail screen
    @GET("movie/{movie_id}")
    Call<Movie> getMovieDetail(
            @Path("movie_id") int movieId,
            @Query("api_key") String apiKey,
            @Query("language") String language
    );

    // Danh sách diễn viên -> Movie Detail screen
    @GET("movie/{movie_id}/credits")
    Call<CreditsResponse> getMovieCredits(
            @Path("movie_id") int movieId,
            @Query("api_key") String apiKey
    );

    // Trailer (key YouTube) -> nhúng WebView ở Movie Detail
    @GET("movie/{movie_id}/videos")
    Call<VideoListResponse> getMovieVideos(
            @Path("movie_id") int movieId,
            @Query("api_key") String apiKey,
            @Query("language") String language
    );

    // Tìm kiếm phim theo tên -> Search screen
    @GET("search/movie")
    Call<MovieResponse> searchMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("query") String query,
            @Query("page") int page
    );

    // Danh sách thể loại -> dùng để hiển thị filter & map genre_id -> tên
    @GET("genre/movie/list")
    Call<GenreListResponse> getGenreList(
            @Query("api_key") String apiKey,
            @Query("language") String language
    );

    // Khám phá phim theo điều kiện (thể loại, năm, sort) -> dùng cho:
    // 1) filter ở Search screen
    // 2) RecommendationEngine (gợi ý cá nhân hóa theo genre weight)
    @GET("discover/movie")
    Call<MovieResponse> discoverMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("with_genres") String withGenres,      // vd "28,12"
            @Query("primary_release_year") Integer year,  // nullable -> Retrofit tự bỏ qua nếu null
            @Query("sort_by") String sortBy,               // vd "vote_average.desc"
            @Query("vote_count.gte") int minVoteCount,      // lọc phim có đủ lượt vote để tránh sai lệch
            @Query("page") int page
    );
}
