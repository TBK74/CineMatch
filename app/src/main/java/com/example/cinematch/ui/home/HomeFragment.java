package com.example.cinematch.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.cinematch.R;
import com.example.cinematch.adapters.MovieAdapter;
import com.example.cinematch.models.Movie;
import com.example.cinematch.network.ApiClient;
import com.example.cinematch.network.response.MovieResponse;
import com.example.cinematch.recommendation.RecommendationEngine;
import com.example.cinematch.utils.Constants;
import com.example.cinematch.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView rvBanner, rvRecommended, rvPopular;
    private android.widget.LinearLayout genreContainer;
    private SwipeRefreshLayout swipeRefresh;
    private MovieAdapter bannerAdapter, recommendedAdapter, popularAdapter;
    private SharedPrefManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefManager = new SharedPrefManager(requireContext());

        rvBanner = view.findViewById(R.id.rvBanner);
        rvRecommended = view.findViewById(R.id.rvRecommended);
        rvPopular = view.findViewById(R.id.rvPopular);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        genreContainer = view.findViewById(R.id.genreContainer);

        setupRecyclerView(rvBanner);
        setupRecyclerView(rvRecommended);
        setupRecyclerView(rvPopular);

        bannerAdapter = new MovieAdapter(requireContext(), new ArrayList<>());
        recommendedAdapter = new MovieAdapter(requireContext(), new ArrayList<>());
        popularAdapter = new MovieAdapter(requireContext(), new ArrayList<>());

        rvBanner.setAdapter(bannerAdapter);
        rvRecommended.setAdapter(recommendedAdapter);
        rvPopular.setAdapter(popularAdapter);

        swipeRefresh.setOnRefreshListener(this::loadAllData);
        loadAllData();
    }

    // Lấy danh sách thể loại từ TMDB, dựng động 4 hàng đầu tiên (Hành động, Hài, Kinh dị...)
    // ngay dưới hàng "Phổ biến" -> đáp ứng yêu cầu "thêm mục thể loại" ở trang chủ.
    private void loadGenreRows() {
        ApiClient.getTmdbApi().getGenreList(Constants.TMDB_API_KEY, Constants.TMDB_LANGUAGE)
                .enqueue(new Callback<com.example.cinematch.network.response.GenreListResponse>() {
                    @Override
                    public void onResponse(Call<com.example.cinematch.network.response.GenreListResponse> call,
                                            Response<com.example.cinematch.network.response.GenreListResponse> response) {
                        if (!isAdded() || !response.isSuccessful() || response.body() == null) return;

                        genreContainer.removeAllViews(); // tránh nhân đôi khi kéo refresh nhiều lần
                        List<com.example.cinematch.models.Genre> genres = response.body().getGenres();

                        int rowCount = Math.min(4, genres.size());
                        for (int i = 0; i < rowCount; i++) {
                            addGenreRow(genres.get(i));
                        }
                    }

                    @Override
                    public void onFailure(Call<com.example.cinematch.network.response.GenreListResponse> call, Throwable t) { }
                });
    }

    // Tạo 1 hàng gồm tiêu đề thể loại + RecyclerView ngang, gọi /discover/movie theo genre đó
    private void addGenreRow(com.example.cinematch.models.Genre genre) {
        TextView title = new TextView(requireContext());
        title.setText(genre.getName());
        title.setTextColor(getResources().getColor(R.color.white));
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        int marginPx = (int) (12 * getResources().getDisplayMetrics().density);
        int marginTopPx = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(marginPx, marginTopPx, 0, 0);
        title.setLayoutParams(titleParams);
        genreContainer.addView(title);

        RecyclerView rv = new RecyclerView(requireContext());
        LinearLayout.LayoutParams rvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (220 * getResources().getDisplayMetrics().density));
        rv.setLayoutParams(rvParams);
        rv.setPadding(marginPx, 0, 0, 0);
        rv.setClipToPadding(false);
        setupRecyclerView(rv);
        genreContainer.addView(rv);

        MovieAdapter adapter = new MovieAdapter(requireContext(), new ArrayList<>());
        rv.setAdapter(adapter);

        ApiClient.getTmdbApi().discoverMovies(Constants.TMDB_API_KEY, Constants.TMDB_LANGUAGE,
                        String.valueOf(genre.getId()), null, "popularity.desc", 20, 1)
                .enqueue(new Callback<MovieResponse>() {
                    @Override
                    public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                        if (isAdded() && response.isSuccessful() && response.body() != null) {
                            adapter.updateData(response.body().getResults());
                        }
                    }

                    @Override
                    public void onFailure(Call<MovieResponse> call, Throwable t) { }
                });
    }

    private void setupRecyclerView(RecyclerView rv) {
        rv.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
    }

    private void loadAllData() {
        loadPopular();
        loadRecommended();
        loadGenreRows();
    }

    // Banner + hàng "Phổ biến" đều lấy từ /movie/popular
    private void loadPopular() {
        ApiClient.getTmdbApi().getPopularMovies(Constants.TMDB_API_KEY, Constants.TMDB_LANGUAGE, 1)
                .enqueue(new Callback<MovieResponse>() {
                    @Override
                    public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                        swipeRefresh.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            List<Movie> movies = response.body().getResults();
                            bannerAdapter.updateData(movies);
                            popularAdapter.updateData(movies);
                        }
                    }

                    @Override
                    public void onFailure(Call<MovieResponse> call, Throwable t) {
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Hàng "Gợi ý cho bạn" dùng RecommendationEngine (genre weight cá nhân hóa)
    private void loadRecommended() {
        String uid = prefManager.getUid();
        if (uid == null) return;

        new RecommendationEngine().getRecommendations(uid, new RecommendationEngine.RecommendationCallback() {
            @Override
            public void onSuccess(List<Movie> recommendedMovies) {
                if (isAdded()) recommendedAdapter.updateData(recommendedMovies);
            }

            @Override
            public void onError(String message) {
                // Không cần chặn UI, chỉ log/toast nhẹ
                if (isAdded()) Toast.makeText(getContext(), "Không tải được gợi ý", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
