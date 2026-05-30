package com.koshub.psdku.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.koshub.psdku.R;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.utils.DatabaseConstants;
import java.util.List;

public class RecentBookingAdapter extends RecyclerView.Adapter<RecentBookingAdapter.ViewHolder> {

    private final List<Booking> bookings;

    public RecentBookingAdapter(List<Booking> bookings) {
        this.bookings = bookings;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_dashboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        holder.tvTenantName.setText(booking.getStudentName());
        holder.tvBookingDetail.setText(booking.getKosName() + " • " + booking.getRoomName());
        
        holder.tvStatusBadge.setText(booking.getStatus().toUpperCase());
        
        int bgRes;
        int textColorRes;
        
        String status = booking.getStatus() != null ? booking.getStatus().toLowerCase() : "";
        switch (status) {
            case "accepted":
            case "waiting_payment":
                bgRes = R.drawable.bg_owner_booking_accepted;
                textColorRes = R.color.owner_booking_accepted_text;
                break;
            case "pending":
            default:
                bgRes = R.drawable.bg_owner_booking_pending;
                textColorRes = R.color.owner_booking_pending_text;
                break;
        }
        
        holder.tvStatusBadge.setBackgroundResource(bgRes);
        holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getResources().getColor(textColorRes));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTenantName, tvBookingDetail, tvStatusBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTenantName = itemView.findViewById(R.id.tvTenantName);
            tvBookingDetail = itemView.findViewById(R.id.tvBookingDetail);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
        }
    }
}
