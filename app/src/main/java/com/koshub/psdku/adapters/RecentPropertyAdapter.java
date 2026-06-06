package com.koshub.psdku.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.koshub.psdku.R;
import com.koshub.psdku.models.Kos;
import java.util.List;

public class RecentPropertyAdapter extends RecyclerView.Adapter<RecentPropertyAdapter.ViewHolder> {

    private final List<Kos> kosList;

    public RecentPropertyAdapter(List<Kos> kosList) {
        this.kosList = kosList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_property_dashboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Kos kos = kosList.get(position);
        holder.tvPropertyName.setText(kos.getName());
        holder.tvPropertyAddress.setText(kos.getAddress());
        holder.tvPropertyRating.setText(String.valueOf(kos.getRatingAverage()));
        holder.tvPropertyRooms.setText(kos.getAvailableRooms() + " kamar tersedia");

        if (kos.getImageUrls() != null && !kos.getImageUrls().isEmpty()) {
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                    .load(kos.getImageUrls().get(0))
                    .placeholder(R.drawable.ic_owner_building)
                    .error(R.drawable.ic_owner_building)
                    .centerCrop()
                    .into(holder.imgKos);
        } else {
            holder.imgKos.setImageResource(R.drawable.ic_owner_building);
        }
        
        // Status badge logic if any
    }

    @Override
    public int getItemCount() {
        return kosList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPropertyName, tvPropertyAddress, tvPropertyRating, tvPropertyRooms;
        android.widget.ImageView imgKos;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPropertyName = itemView.findViewById(R.id.tvPropertyName);
            tvPropertyAddress = itemView.findViewById(R.id.tvPropertyAddress);
            tvPropertyRating = itemView.findViewById(R.id.tvPropertyRating);
            tvPropertyRooms = itemView.findViewById(R.id.tvPropertyRooms);
            imgKos = itemView.findViewById(R.id.imgKos);
        }
    }
}
