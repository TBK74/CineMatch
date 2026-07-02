package com.example.cinematch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cinematch.R;
import com.example.cinematch.models.Cast;

import java.util.List;

// Adapter hiển thị hàng diễn viên ở Movie Detail.
public class CastAdapter extends RecyclerView.Adapter<CastAdapter.CastViewHolder> {

    private final Context context;
    private final List<Cast> castList;

    public CastAdapter(Context context, List<Cast> castList) {
        this.context = context;
        this.castList = castList;
    }

    public void updateData(List<Cast> newList) {
        castList.clear();
        castList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cast, parent, false);
        return new CastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CastViewHolder holder, int position) {
        Cast cast = castList.get(position);
        holder.tvName.setText(cast.getName());
        Glide.with(context)
                .load(cast.getFullProfileUrl())
                .placeholder(R.drawable.placeholder_poster)
                .into(holder.imgActor);
    }

    @Override
    public int getItemCount() {
        return castList.size();
    }

    static class CastViewHolder extends RecyclerView.ViewHolder {
        ImageView imgActor;
        TextView tvName;

        CastViewHolder(@NonNull View itemView) {
            super(itemView);
            imgActor = itemView.findViewById(R.id.imgActor);
            tvName = itemView.findViewById(R.id.tvActorName);
        }
    }
}
