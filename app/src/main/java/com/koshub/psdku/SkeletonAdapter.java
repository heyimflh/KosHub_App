package com.koshub.psdku;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SkeletonAdapter extends RecyclerView.Adapter<SkeletonAdapter.SkeletonViewHolder> {

    @NonNull
    @Override
    public SkeletonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_kos_skeleton, parent, false);
        return new SkeletonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SkeletonViewHolder holder, int position) {
        // No data to bind for skeleton
    }

    @Override
    public int getItemCount() {
        return 3; // Show 3 skeleton items
    }

    static class SkeletonViewHolder extends RecyclerView.ViewHolder {
        SkeletonViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
