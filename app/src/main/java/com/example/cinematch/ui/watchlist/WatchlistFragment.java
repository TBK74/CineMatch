package com.example.cinematch.ui.watchlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cinematch.R;
import com.example.cinematch.adapters.WatchlistAdapter;
import com.example.cinematch.firebase.FirestoreManager;
import com.example.cinematch.models.WatchlistItem;
import com.example.cinematch.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.List;

public class WatchlistFragment extends Fragment {

    private RecyclerView rvWatchlist;
    private TextView tvEmpty;
    private WatchlistAdapter adapter;
    private FirestoreManager firestoreManager;
    private SharedPrefManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_watchlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestoreManager = new FirestoreManager();
        prefManager = new SharedPrefManager(requireContext());

        rvWatchlist = view.findViewById(R.id.rvWatchlist);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        rvWatchlist.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new WatchlistAdapter(requireContext(), new ArrayList<>(), (item, position) -> {
            firestoreManager.removeFromWatchlist(item.getUserId(), item.getMovieId(),
                    new FirestoreManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            if (isAdded()) adapter.removeAt(position);
                        }

                        @Override
                        public void onError(String message) {
                            if (isAdded()) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        rvWatchlist.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWatchlist(); // reload mỗi lần quay lại tab để cập nhật item vừa thêm/xóa ở Detail
    }

    private void loadWatchlist() {
        String uid = prefManager.getUid();
        if (uid == null) return;

        firestoreManager.getWatchlist(uid, new FirestoreManager.ListCallback<WatchlistItem>() {
            @Override
            public void onSuccess(List<WatchlistItem> list) {
                if (!isAdded()) return;
                adapter.updateData(list);
                tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
