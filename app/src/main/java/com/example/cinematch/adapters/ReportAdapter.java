package com.example.cinematch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cinematch.R;
import com.example.cinematch.models.Review;

import java.util.List;

// Adapter cho Moderator Dashboard: hiển thị review bị báo cáo, có nút Duyệt / Xóa.
public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    public interface OnModerateActionListener {
        void onApprove(Review review, int position); // giữ lại review
        void onHide(Review review, int position);     // ẩn/xóa review vi phạm
    }

    private final Context context;
    private final List<Review> reportedReviews;
    private final OnModerateActionListener listener;

    public ReportAdapter(Context context, List<Review> reportedReviews, OnModerateActionListener listener) {
        this.context = context;
        this.reportedReviews = reportedReviews;
        this.listener = listener;
    }

    public void updateData(List<Review> newList) {
        reportedReviews.clear();
        reportedReviews.addAll(newList);
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        reportedReviews.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Review review = reportedReviews.get(position);
        holder.tvUserName.setText(review.getUserName());
        holder.tvContent.setText(review.getContent());
        holder.tvReportCount.setText(context.getString(R.string.report_count_format, review.getReportCount()));

        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(review, holder.getAdapterPosition());
        });
        holder.btnHide.setOnClickListener(v -> {
            if (listener != null) listener.onHide(review, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return reportedReviews.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvContent, tvReportCount;
        Button btnApprove, btnHide;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvReportCount = itemView.findViewById(R.id.tvReportCount);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnHide = itemView.findViewById(R.id.btnHide);
        }
    }
}
