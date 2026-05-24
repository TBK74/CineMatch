package com.example.cinematch.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cinematch.R;
import com.example.cinematch.models.Movie;
import com.example.cinematch.ui.detail.MovieDetailActivity;

import java.util.List;

// Adapter dùng chung cho Home (banner, hàng gợi ý, hàng thể loại) và Search results.
public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private final Context context;
    private final List<Movie> movieList;
    private final boolean isGrid;

    public MovieAdapter(Context context, List<Movie> movieList) {
        this(context, movieList, false);
    }

    // isGrid = true -> dùng item_movie_grid.xml (match_parent + tỉ lệ khung hình, tránh đè lên
    // nhau khi hiển thị trong GridLayoutManager, vd màn Search). isGrid = false -> item_movie.xml
    // (width cố định 120dp, dùng cho các hàng cuộn ngang như Home/Profile/Similar).
    public MovieAdapter(Context context, List<Movie> movieList, boolean isGrid) {
        this.context = context;
        this.movieList = movieList;
        this.isGrid = isGrid;
    }

    public void updateData(List<Movie> newList) {
        movieList.clear();
        movieList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = isGrid ? R.layout.item_movie_grid : R.layout.item_movie;
        View view = LayoutInflater.from(context).inflate(layoutRes, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieList.get(position);
        holder.tvTitle.setText(movie.getTitle());
        holder.tvRating.setText(String.format("%.1f", movie.getVoteAverage()));

        Glide.with(context)
                .load(movie.getFullPosterUrl())
                .placeholder(R.drawable.placeholder_poster)
                .into(holder.imgPoster);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra(MovieDetailActivity.EXTRA_MOVIE_ID, movie.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster;
        TextView tvTitle;
        TextView tvRating;

        MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgPoster);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvRating = itemView.findViewById(R.id.tvRating);
        }
    }
}
