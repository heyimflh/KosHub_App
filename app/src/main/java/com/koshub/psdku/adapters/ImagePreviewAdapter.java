package com.koshub.psdku.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.koshub.psdku.R;

import java.util.List;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder> {

    private final List<Object> imageList;
    private final OnImageRemoveListener listener;

    public interface OnImageRemoveListener {
        void onRemove(int position);
    }

    public ImagePreviewAdapter(List<Object> imageList, OnImageRemoveListener listener) {
        this.imageList = imageList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = imageList.get(position);
        
        Glide.with(holder.imgPreview.getContext())
                .load(item)
                .placeholder(R.drawable.bg_map_placeholder)
                .into(holder.imgPreview);

        holder.tvCoverBadge.setVisibility(position == 0 ? View.VISIBLE : View.GONE);

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemove(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPreview;
        ImageButton btnRemove;
        TextView tvCoverBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.imgPreview);
            btnRemove = itemView.findViewById(R.id.btnRemoveImage);
            tvCoverBadge = itemView.findViewById(R.id.tvCoverBadge);
        }
    }
}
