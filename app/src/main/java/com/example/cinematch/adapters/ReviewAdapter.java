package com.example.cinematch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cinematch.R;
import com.example.cinematch.models.Review;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

// Adapter hiển thị danh sách review cộng đồng ở Movie Detail, có nút "Báo cáo".
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    public interface OnReportClickListener {
        void onReport(Review review);
    }

    private final Context context;
    private final List<Review> reviewList;
    private final OnReportClickListener reportClickListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public ReviewAdapter(Context context, List<Review> reviewList, OnReportClickListener listener) {
        this.context = context;
        this.reviewList = reviewList;
        this.reportClickListener = listener;
    }

    public void updateData(List<Review> newList) {
        reviewList.clear();
        reviewList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);
        holder.tvUserName.setText(review.getUserName());
        holder.tvContent.setText(review.getContent());
        holder.tvDate.setText(dateFormat.format(review.getCreatedAt()));

        holder.btnReport.setOnClickListener(v -> {
            if (reportClickListener != null) reportClickListener.onReport(review);
        });
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvContent, tvDate, btnReport;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnReport = itemView.findViewById(R.id.btnReport);
        }
    }
}
