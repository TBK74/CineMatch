package com.example.cinematch.recommendation;

import com.example.cinematch.firebase.FirestoreManager;
import com.example.cinematch.models.Movie;
import com.example.cinematch.models.Rating;
import com.example.cinematch.network.ApiClient;
import com.example.cinematch.network.response.MovieResponse;
import com.example.cinematch.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Tính "genre weight" từ lịch sử rating của user, gọi TMDB /discover/movie theo genre
// có weight cao nhất, loại phim đã xem, sort theo vote_average kết hợp genre_match_score.
public class RecommendationEngine {

    public interface RecommendationCallback {
        void onSuccess(List<Movie> recommendedMovies);
        void onError(String message);
    }

    private final FirestoreManager firestoreManager;

    public RecommendationEngine() {
        this.firestoreManager = new FirestoreManager();
    }

    public void getRecommendations(String userId, RecommendationCallback callback) {
        firestoreManager.getUserRatings(userId, new FirestoreManager.ListCallback<Rating>() {
            @Override
            public void onSuccess(List<Rating> ratings) {
                if (ratings.isEmpty()) {
                    // User mới chưa rate phim nào -> fallback lấy phim hot chung
                    fetchFallbackPopular(callback);
                    return;
                }

                Map<Integer, Double> genreWeights = calculateGenreWeights(ratings);
                Set<Integer> watchedMovieIds = new HashSet<>();
                for (Rating r : ratings) watchedMovieIds.add(r.getMovieId());

                // Lấy top 2 genre có weight cao nhất để gọi discover
                List<Integer> topGenres = getTopGenres(genreWeights, 2);
                String genreParam = joinIds(topGenres);

                fetchDiscoverMovies(genreParam, genreWeights, watchedMovieIds, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    // ===== Bước 1: Tính genre weight =====
    // weight(genre) = trung bình điểm rating của user cho các phim thuộc genre đó
    // Genre nào được rate cao & nhiều lần -> weight cao -> ưu tiên gợi ý
    private Map<Integer, Double> calculateGenreWeights(List<Rating> ratings) {
        Map<Integer, Double> totalScore = new HashMap<>();
        Map<Integer, Integer> countPerGenre = new HashMap<>();

        for (Rating rating : ratings) {
            if (rating.getGenreIds() == null) continue;
            for (Integer genreId : rating.getGenreIds()) {
                totalScore.put(genreId, totalScore.getOrDefault(genreId, 0.0) + rating.getScore());
                countPerGenre.put(genreId, countPerGenre.getOrDefault(genreId, 0) + 1);
            }
        }

        Map<Integer, Double> weights = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : totalScore.entrySet()) {
            int genreId = entry.getKey();
            double avg = entry.getValue() / countPerGenre.get(genreId);
            weights.put(genreId, avg);
        }
        return weights;
    }

    // Lấy N genre có weight cao nhất
    private List<Integer> getTopGenres(Map<Integer, Double> weights, int topN) {
        List<Map.Entry<Integer, Double>> entries = new ArrayList<>(weights.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, entries.size()); i++) {
            result.add(entries.get(i).getKey());
        }
        return result;
    }

    private String joinIds(List<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(ids.get(i));
        }
        return sb.toString();
    }

    // ===== Bước 2: Gọi TMDB /discover/movie theo genre weight cao nhất =====
    private void fetchDiscoverMovies(String genreParam, Map<Integer, Double> genreWeights,
                                      Set<Integer> watchedMovieIds, RecommendationCallback callback) {

        Call<MovieResponse> call = ApiClient.getTmdbApi().discoverMovies(
                Constants.TMDB_API_KEY,
                Constants.TMDB_LANGUAGE,
                genreParam,
                null,
                "vote_average.desc",
                100, // chỉ lấy phim có đủ 100 lượt vote trở lên để tránh phim ít người biết bị lệch điểm
                1
        );

        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Không lấy được danh sách gợi ý (code " + response.code() + ")");
                    return;
                }

                List<Movie> candidates = response.body().getResults();

                // ===== Bước 3: Loại phim đã xem + tính genre_match_score + sort =====
                List<Movie> result = new ArrayList<>();
                for (Movie movie : candidates) {
                    if (watchedMovieIds.contains(movie.getId())) continue; // loại phim đã rate rồi

                    double matchScore = 0;
                    if (movie.getGenreIds() != null) {
                        for (Integer genreId : movie.getGenreIds()) {
                            matchScore += genreWeights.getOrDefault(genreId, 0.0);
                        }
                    }
                    movie.setGenreMatchScore(matchScore);
                    result.add(movie);
                }

                // Sort kết hợp: ưu tiên genre_match_score trước, vote_average làm tiêu chí phụ
                result.sort((a, b) -> {
                    int cmp = Double.compare(b.getGenreMatchScore(), a.getGenreMatchScore());
                    if (cmp != 0) return cmp;
                    return Double.compare(b.getVoteAverage(), a.getVoteAverage());
                });

                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    // User chưa có rating nào -> trả về phim hot chung (không cá nhân hóa được)
    private void fetchFallbackPopular(RecommendationCallback callback) {
        ApiClient.getTmdbApi()
                .getPopularMovies(Constants.TMDB_API_KEY, Constants.TMDB_LANGUAGE, 1)
                .enqueue(new Callback<MovieResponse>() {
                    @Override
                    public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().getResults());
                        } else {
                            callback.onError("Không lấy được danh sách phim hot");
                        }
                    }

                    @Override
                    public void onFailure(Call<MovieResponse> call, Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }
}
