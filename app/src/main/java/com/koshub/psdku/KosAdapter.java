package com.koshub.psdku;

import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.koshub.psdku.repositories.CloudinaryRepository;

import java.util.List;
import java.util.Locale;

public class KosAdapter extends RecyclerView.Adapter<KosAdapter.KosViewHolder> {

    private List<KosItem> kosList;
    private OnKosClickListener listener;

    public interface OnKosClickListener {
        void onKosClick(KosItem item, int position);
        void onFavoriteClick(KosItem item, int position);
    }

    public KosAdapter(List<KosItem> kosList, OnKosClickListener listener) {
        this.kosList = (kosList != null) ? kosList : new java.util.ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public KosViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_kos_card, parent, false);
        return new KosViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KosViewHolder holder, int position) {
        KosItem item = kosList.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return (kosList != null) ? kosList.size() : 0;
    }

    public void updateList(List<KosItem> newList) {
        this.kosList = newList;
        notifyDataSetChanged();
    }

    class KosViewHolder extends RecyclerView.ViewHolder {
        ImageView imgKos, icFavorite;
        TextView tvBadgeCategory, tvBadgePremium, tvBadgeSisa;
        TextView tvKosName, tvRating, tvAddress, tvPrice, tvDistance;
        LinearLayout chipContainer;
        FrameLayout btnFavorite;

        KosViewHolder(@NonNull View itemView) {
            super(itemView);
            imgKos = itemView.findViewById(R.id.imgKos);
            icFavorite = itemView.findViewById(R.id.icFavorite);
            tvBadgeCategory = itemView.findViewById(R.id.tvBadgeCategory);
            tvBadgePremium = itemView.findViewById(R.id.tvBadgePremium);
            tvBadgeSisa = itemView.findViewById(R.id.tvBadgeSisa);
            tvKosName = itemView.findViewById(R.id.tvKosName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            chipContainer = itemView.findViewById(R.id.chipContainer);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }

        void bind(KosItem item, int position) {
            // Image (Optimized via Cloudinary)
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                String optimizedUrl = CloudinaryRepository.getInstance().getOptimizedUrl(item.getImageUrl(), 500, 300, false);
                Glide.with(itemView.getContext())
                        .load(optimizedUrl)
                        .placeholder(R.drawable.bg_map_placeholder)
                        .error(R.drawable.bg_map_placeholder)
                        .into(imgKos);
            } else {
                imgKos.setImageResource(item.getImageRes());
            }

            // Badge category
            tvBadgeCategory.setText(item.getCategory());

            // Premium badge
            tvBadgePremium.setVisibility(item.isPremium() ? View.VISIBLE : View.GONE);

            // Sisa kamar badge
            if (item.getSisaKamar() != null && !item.getSisaKamar().isEmpty()) {
                tvBadgeSisa.setText(item.getSisaKamar());
                tvBadgeSisa.setVisibility(View.VISIBLE);
            } else {
                tvBadgeSisa.setVisibility(View.GONE);
            }

            // Content
            tvKosName.setText(item.getName());
            double avg = item.getRatingAverage();
            if (avg > 0) {
                tvRating.setText(String.format(Locale.getDefault(), "%.1f", avg));
            } else {
                tvRating.setText("—");
            }
            tvAddress.setText(item.getAddress());
            tvPrice.setText(item.getPrice());
            tvDistance.setText(item.getDistance());

            // Favorite state
            icFavorite.setImageResource(item.isFavorite() ?
                    R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

            // Facility chips
            chipContainer.removeAllViews();
            for (String facility : item.getFacilities()) {
                TextView chip = new TextView(itemView.getContext());
                chip.setText(facility);
                chip.setTextSize(11);
                chip.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.home_text_secondary));
                chip.setBackgroundResource(R.drawable.bg_chip_facility);
                chip.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
                chip.setTypeface(null, android.graphics.Typeface.NORMAL);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMarginEnd(dpToPx(6));
                chip.setLayoutParams(params);
                chipContainer.addView(chip);
            }

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onKosClick(item, position);
            });

            btnFavorite.setOnClickListener(v -> {
                item.setFavorite(!item.isFavorite());
                icFavorite.setImageResource(item.isFavorite() ?
                        R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

                // Scale animation
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(btnFavorite, "scaleX", 1f, 1.2f, 1f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(btnFavorite, "scaleY", 1f, 1.2f, 1f);
                scaleX.setDuration(250);
                scaleY.setDuration(250);
                scaleX.start();
                scaleY.start();

                if (listener != null) listener.onFavoriteClick(item, position);
            });

            // Card press effect
            itemView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(100).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        break;
                }
                return false;
            });
        }

        private int dpToPx(int dp) {
            return (int) (dp * itemView.getContext().getResources().getDisplayMetrics().density);
        }
    }
}
