package com.koshub.psdku.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.koshub.psdku.R;
import com.koshub.psdku.repositories.CloudinaryRepository;

import java.util.List;

public class PropertyImageAdapter extends RecyclerView.Adapter<PropertyImageAdapter.ViewHolder> {

    private final List<String> imageUrls;
    private final int fallbackRes;

    public PropertyImageAdapter(List<String> imageUrls, int fallbackRes) {
        this.imageUrls = imageUrls;
        this.fallbackRes = fallbackRes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_property_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = imageUrls.get(position);
        int placeholder = fallbackRes != 0 ? fallbackRes : R.drawable.bg_map_placeholder;

        if (url != null && !url.isEmpty()) {
            String optimizedUrl = CloudinaryRepository.getInstance().getOptimizedUrl(url, 800, 500, false);
            Glide.with(holder.imageView.getContext())
                    .load(optimizedUrl)
                    .placeholder(placeholder)
                    .error(placeholder)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivPropertyImage);
        }
    }
}
