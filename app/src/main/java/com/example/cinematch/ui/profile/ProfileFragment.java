package com.example.cinematch.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cinematch.R;
import com.example.cinematch.adapters.MovieAdapter;
import com.example.cinematch.firebase.AuthManager;
import com.example.cinematch.firebase.FirestoreManager;
import com.example.cinematch.models.Genre;
import com.example.cinematch.models.Movie;
import com.example.cinematch.models.Rating;
import com.example.cinematch.models.User;
import com.example.cinematch.models.WatchlistItem;
import com.example.cinematch.network.ApiClient;
import com.example.cinematch.network.response.GenreListResponse;
import com.example.cinematch.ui.auth.LoginActivity;
import com.example.cinematch.ui.moderator.ModeratorDashboardActivity;
import com.example.cinematch.utils.Constants;
import com.example.cinematch.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView tvDisplayName, tvEmail, tvExtraInfo, tvRatedCount, tvWatchlistCount, tvAvgScore, tvFavoriteGenre, tvNoRatings;
    private android.widget.ImageView imgAvatar;
    private Button btnModeratorDashboard, btnLogout, btnEditProfile;
    private RecyclerView rvRatedMovies;
    private MovieAdapter ratedAdapter;

    private FirestoreManager firestoreManager;
    private AuthManager authManager;
    private SharedPrefManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestoreManager = new FirestoreManager();
        authManager = new AuthManager(requireContext());
        prefManager = new SharedPrefManager(requireContext());

        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvExtraInfo = view.findViewById(R.id.tvExtraInfo);
        tvRatedCount = view.findViewById(R.id.tvRatedCount);
        tvWatchlistCount = view.findViewById(R.id.tvWatchlistCount);
        tvAvgScore = view.findViewById(R.id.tvAvgScore);
        tvFavoriteGenre = view.findViewById(R.id.tvFavoriteGenre);
        tvNoRatings = view.findViewById(R.id.tvNoRatings);
        rvRatedMovies = view.findViewById(R.id.rvRatedMovies);
        btnModeratorDashboard = view.findViewById(R.id.btnModeratorDashboard);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        rvRatedMovies.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        ratedAdapter = new MovieAdapter(requireContext(), new ArrayList<>());
        rvRatedMovies.setAdapter(ratedAdapter);

        btnModeratorDashboard.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ModeratorDashboardActivity.class)));

        btnLogout.setOnClickListener(v -> {
            authManager.logout();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        loadStatsAndRatings();
    }

    private void loadProfile() {
        String uid = prefManager.getUid();
        if (uid == null) return;

        firestoreManager.getUserProfile(uid, new FirestoreManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) return;
                tvDisplayName.setText(user.getDisplayName());
                tvEmail.setText(user.getEmail());

                StringBuilder extra = new StringBuilder();
                if (user.getGender() != null) extra.append(user.getGender());
                if (user.getAge() > 0) extra.append(extra.length() > 0 ? " • " : "").append(user.getAge()).append(" tuổi");
                if (user.getPhone() != null && !user.getPhone().isEmpty())
                    extra.append(extra.length() > 0 ? " • " : "").append(user.getPhone());
                tvExtraInfo.setText(extra.toString());

                if (user.getAvatarUrl() != null) {
                    com.bumptech.glide.Glide.with(requireContext()).load(user.getAvatarUrl())
                            .circleCrop().placeholder(R.drawable.bg_avatar_circle).into(imgAvatar);
                }

                btnModeratorDashboard.setVisibility(user.isModerator() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Thống kê: số phim đã rate, watchlist, điểm trung bình, thể loại yêu thích + danh sách phim đã rate
    private void loadStatsAndRatings() {
        String uid = prefManager.getUid();
        if (uid == null) return;

        firestoreManager.getUserRatings(uid, new FirestoreManager.ListCallback<Rating>() {
            @Override
            public void onSuccess(List<Rating> ratings) {
                if (!isAdded()) return;

                tvRatedCount.setText(String.valueOf(ratings.size()));
                tvNoRatings.setVisibility(ratings.isEmpty() ? View.VISIBLE : View.GONE);
                rvRatedMovies.setVisibility(ratings.isEmpty() ? View.GONE : View.VISIBLE);

                if (!ratings.isEmpty()) {
                    double sum = 0;
                    for (Rating r : ratings) sum += r.getScore();
                    tvAvgScore.setText(String.format(Locale.getDefault(), "%.1f", sum / ratings.size()));

                    // Chuyển Rating -> Movie "giả" để tái dùng MovieAdapter/item_movie.xml có sẵn
                    List<Movie> ratedMovies = new ArrayList<>();
                    for (Rating r : ratings) {
                        Movie m = new Movie();
                        m.setId(r.getMovieId());
                        m.setTitle(r.getMovieTitle());
                        m.setPosterPath(r.getPosterPath());
                        m.setVoteAverage(r.getScore());
                        ratedMovies.add(m);
                    }
                    ratedAdapter.updateData(ratedMovies);

                    loadFavoriteGenre(ratings);
                } else {
                    tvAvgScore.setText("--");
                    tvFavoriteGenre.setText("");
                }

                loadWatchlistCount(uid);
            }

            @Override
            public void onError(String message) { }
        });
    }

    // Tính thể loại được rate cao nhất trung bình -> hiện dòng "Bạn thích nhất: Hành động"
    private void loadFavoriteGenre(List<Rating> ratings) {
        Map<Integer, Double> totalScore = new HashMap<>();
        Map<Integer, Integer> count = new HashMap<>();
        for (Rating r : ratings) {
            if (r.getGenreIds() == null) continue;
            for (Integer genreId : r.getGenreIds()) {
                totalScore.put(genreId, totalScore.getOrDefault(genreId, 0.0) + r.getScore());
                count.put(genreId, count.getOrDefault(genreId, 0) + 1);
            }
        }
        if (totalScore.isEmpty()) return;

        int bestGenreId = -1;
        double bestAvg = -1;
        for (Map.Entry<Integer, Double> entry : totalScore.entrySet()) {
            double avg = entry.getValue() / count.get(entry.getKey());
            if (avg > bestAvg) {
                bestAvg = avg;
                bestGenreId = entry.getKey();
            }
        }

        int finalBestGenreId = bestGenreId;
        ApiClient.getTmdbApi().getGenreList(Constants.TMDB_API_KEY, Constants.TMDB_LANGUAGE)
                .enqueue(new Callback<GenreListResponse>() {
                    @Override
                    public void onResponse(Call<GenreListResponse> call, Response<GenreListResponse> response) {
                        if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                        for (Genre g : response.body().getGenres()) {
                            if (g.getId() == finalBestGenreId) {
                                tvFavoriteGenre.setText("Thể loại bạn đánh giá cao nhất: " + g.getName());
                                break;
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<GenreListResponse> call, Throwable t) { }
                });
    }

    private void loadWatchlistCount(String uid) {
        firestoreManager.getWatchlist(uid, new FirestoreManager.ListCallback<WatchlistItem>() {
            @Override
            public void onSuccess(List<WatchlistItem> list) {
                if (isAdded()) tvWatchlistCount.setText(String.valueOf(list.size()));
            }

            @Override
            public void onError(String message) { }
        });
    }
}
