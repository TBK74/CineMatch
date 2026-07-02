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

import com.example.cinematch.R;
import com.example.cinematch.firebase.AuthManager;
import com.example.cinematch.firebase.FirestoreManager;
import com.example.cinematch.models.Rating;
import com.example.cinematch.models.User;
import com.example.cinematch.models.WatchlistItem;
import com.example.cinematch.ui.auth.LoginActivity;
import com.example.cinematch.ui.moderator.ModeratorDashboardActivity;
import com.example.cinematch.utils.SharedPrefManager;

import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView tvDisplayName, tvEmail, tvStats;
    private Button btnModeratorDashboard, btnLogout;

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

        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvStats = view.findViewById(R.id.tvStats);
        btnModeratorDashboard = view.findViewById(R.id.btnModeratorDashboard);
        btnLogout = view.findViewById(R.id.btnLogout);

        loadProfile();
        loadStats();

        btnModeratorDashboard.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ModeratorDashboardActivity.class)));

        btnLogout.setOnClickListener(v -> {
            authManager.logout();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
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
                // Chỉ moderator mới thấy nút vào Dashboard duyệt review
                btnModeratorDashboard.setVisibility(user.isModerator() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Thống kê cá nhân: số phim đã rate + số phim trong watchlist
    private void loadStats() {
        String uid = prefManager.getUid();
        if (uid == null) return;

        firestoreManager.getUserRatings(uid, new FirestoreManager.ListCallback<Rating>() {
            @Override
            public void onSuccess(List<Rating> ratings) {
                if (!isAdded()) return;
                firestoreManager.getWatchlist(uid, new FirestoreManager.ListCallback<WatchlistItem>() {
                    @Override
                    public void onSuccess(List<WatchlistItem> watchlist) {
                        if (!isAdded()) return;
                        tvStats.setText("Đã đánh giá " + ratings.size() + " phim  •  "
                                + watchlist.size() + " phim trong watchlist");
                    }

                    @Override
                    public void onError(String message) { }
                });
            }

            @Override
            public void onError(String message) { }
        });
    }
}
