package com.example.cinematch.ui.search;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cinematch.R;
import com.example.cinematch.adapters.MovieAdapter;
import com.example.cinematch.database.MovieCacheDAO;
import com.example.cinematch.models.Movie;
import com.example.cinematch.network.ApiClient;
import com.example.cinematch.network.response.GenreListResponse;
import com.example.cinematch.network.response.MovieResponse;
import com.example.cinematch.utils.Constants;
import com.example.cinematch.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private EditText edtSearch;
    private RecyclerView rvResults;
    private MovieAdapter adapter;
    private MovieCacheDAO cacheDAO;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    // Filter state (null = không áp dụng)
    private String filterGenreId = null;
    private Integer filterYear = null;
    private Double filterMinScore = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cacheDAO = new MovieCacheDAO(requireContext());
        edtSearch = view.findViewById(R.id.edtSearch);
        rvResults = view.findViewById(R.id.rvSearchResults);
        ImageButton btnFilter = view.findViewById(R.id.btnFilter);

        rvResults.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        adapter = new MovieAdapter(requireContext(), new ArrayList<>(), true);
        rvResults.setAdapter(adapter);

        // Tìm kiếm realtime: debounce 500ms để tránh gọi API liên tục mỗi lần gõ phím
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> performSearch(s.toString().trim());
                debounceHandler.postDelayed(debounceRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        btnFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            adapter.updateData(new ArrayList<>());
            return;
        }

        // Lưu lịch sử tìm kiếm vào SQLite trên background thread
        ThreadUtils.runInBackground(() -> cacheDAO.addSearchHistory(query));

        ApiClient.getTmdbApi().searchMovies(Constants.TMDB_API_KEY, Constants.TMDB_LANGUAGE, query, 1)
                .enqueue(new Callback<MovieResponse>() {
                    @Override
                    public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            List<Movie> results = applyLocalFilter(response.body().getResults());
                            adapter.updateData(results);
                        }
                    }

                    @Override
                    public void onFailure(Call<MovieResponse> call, Throwable t) {
                        if (isAdded()) Toast.makeText(getContext(), "Lỗi tìm kiếm: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Lọc thêm ở phía client theo năm/điểm (search endpoint của TMDB không hỗ trợ filter genre/year trực tiếp)
    private List<Movie> applyLocalFilter(List<Movie> movies) {
        List<Movie> filtered = new ArrayList<>();
        for (Movie m : movies) {
            if (filterYear != null) {
                if (m.getReleaseDate() == null || !m.getReleaseDate().startsWith(String.valueOf(filterYear))) continue;
            }
            if (filterMinScore != null && m.getVoteAverage() < filterMinScore) continue;
            if (filterGenreId != null) {
                if (m.getGenreIds() == null || !m.getGenreIds().contains(Integer.parseInt(filterGenreId))) continue;
            }
            filtered.add(m);
        }
        return filtered;
    }

    // Dialog đơn giản cho phép nhập năm + điểm tối thiểu, và chọn thể loại từ TMDB genre list
    private void showFilterDialog() {
        ApiClient.getTmdbApi().getGenreList(Constants.TMDB_API_KEY, Constants.TMDB_LANGUAGE)
                .enqueue(new Callback<GenreListResponse>() {
                    @Override
                    public void onResponse(Call<GenreListResponse> call, Response<GenreListResponse> response) {
                        if (!isAdded() || !response.isSuccessful() || response.body() == null) return;

                        View dialogView = LayoutInflater.from(requireContext())
                                .inflate(android.R.layout.select_dialog_item, null); // dùng layout đơn giản có sẵn của Android
                        EditText edtYear = new EditText(requireContext());
                        edtYear.setHint("Năm phát hành (vd: 2024)");
                        EditText edtMinScore = new EditText(requireContext());
                        edtMinScore.setHint("Điểm tối thiểu (vd: 7.0)");

                        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
                        container.setOrientation(android.widget.LinearLayout.VERTICAL);
                        container.setPadding(32, 16, 32, 16);
                        container.addView(edtYear);
                        container.addView(edtMinScore);

                        new AlertDialog.Builder(requireContext())
                                .setTitle("Lọc kết quả")
                                .setView(container)
                                .setPositiveButton("Áp dụng", (dialog, which) -> {
                                    String yearStr = edtYear.getText().toString().trim();
                                    String scoreStr = edtMinScore.getText().toString().trim();
                                    filterYear = yearStr.isEmpty() ? null : Integer.parseInt(yearStr);
                                    filterMinScore = scoreStr.isEmpty() ? null : Double.parseDouble(scoreStr);
                                    performSearch(edtSearch.getText().toString().trim());
                                })
                                .setNegativeButton("Xóa lọc", (dialog, which) -> {
                                    filterYear = null;
                                    filterMinScore = null;
                                    filterGenreId = null;
                                    performSearch(edtSearch.getText().toString().trim());
                                })
                                .show();
                    }

                    @Override
                    public void onFailure(Call<GenreListResponse> call, Throwable t) {
                        Toast.makeText(getContext(), "Không tải được danh sách thể loại", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
