package com.example.cinematch.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cinematch.R;
import com.example.cinematch.models.WatchlistItem;
import com.example.cinematch.ui.detail.MovieDetailActivity;

import java.util.List;

// Adapter cho màn Watchlist, có nút xóa nhanh khỏi danh sách.
public class WatchlistAdapter extends RecyclerView.Adapter<WatchlistAdapter.WatchlistViewHolder> {

    public interface OnRemoveClickListener {
        void onRemove(WatchlistItem item, int position);
    }

    private final Context context;
    private final List<WatchlistItem> items;
    private final OnRemoveClickListener removeListener;

    public WatchlistAdapter(Context context, List<WatchlistItem> items, OnRemoveClickListener listener) {
        this.context = context;
        this.items = items;
        this.removeListener = listener;
    }

    public void updateData(List<WatchlistItem> newList) {
        items.clear();
        items.addAll(newList);
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public WatchlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_watchlist, parent, false);
        return new WatchlistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WatchlistViewHolder holder, int position) {
        WatchlistItem item = items.get(position);
        holder.tvTitle.setText(item.getMovieTitle());

        String posterUrl = item.getPosterPath() != null
                ? "https://image.tmdb.org/t/p/w500" + item.getPosterPath() : null;
        Glide.with(context).load(posterUrl).placeholder(R.drawable.placeholder_poster).into(holder.imgPoster);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra(MovieDetailActivity.EXTRA_MOVIE_ID, item.getMovieId());
            context.startActivity(intent);
        });

        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null) removeListener.onRemove(item, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class WatchlistViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster;
        TextView tvTitle;
        ImageButton btnRemove;

        WatchlistViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgPoster);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
