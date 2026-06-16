package com.example.cinematch.ui.moderator;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cinematch.R;
import com.example.cinematch.adapters.ReportAdapter;
import com.example.cinematch.firebase.FirestoreManager;
import com.example.cinematch.models.Review;

import java.util.ArrayList;
import java.util.List;

// Chỉ mở màn này khi role = moderator (đã check ở ProfileFragment trước khi hiện nút).
public class ModeratorDashboardActivity extends AppCompatActivity {

    private RecyclerView rvReports;
    private TextView tvEmpty;
    private ReportAdapter adapter;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moderator_dashboard);

        firestoreManager = new FirestoreManager();
        rvReports = findViewById(R.id.rvReports);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvReports.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ReportAdapter(this, new ArrayList<>(), new ReportAdapter.OnModerateActionListener() {
            @Override
            public void onApprove(Review review, int position) {
                firestoreManager.dismissReports(review.getReviewId(), new FirestoreManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(ModeratorDashboardActivity.this, "Đã duyệt review", Toast.LENGTH_SHORT).show();
                        adapter.removeAt(position);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(ModeratorDashboardActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onHide(Review review, int position) {
                firestoreManager.hideReview(review.getReviewId(), new FirestoreManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(ModeratorDashboardActivity.this, "Đã ẩn review vi phạm", Toast.LENGTH_SHORT).show();
                        adapter.removeAt(position);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(ModeratorDashboardActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        rvReports.setAdapter(adapter);

        loadReportedReviews();
    }

    private void loadReportedReviews() {
        firestoreManager.getReportedReviews(new FirestoreManager.ListCallback<Review>() {
            @Override
            public void onSuccess(List<Review> list) {
                adapter.updateData(list);
                tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ModeratorDashboardActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
