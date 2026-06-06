package com.koshub.psdku.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.koshub.psdku.R;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.utils.CurrencyHelper;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.utils.DateHelper;

import java.util.List;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.ViewHolder> {

    private final List<Booking> bookingList;
    private final OnBookingClickListener listener;

    public interface OnBookingClickListener {
        void onActionClick(Booking booking);
        void onReviewClick(Booking booking);
        void onItemClick(Booking booking);
    }

    public BookingHistoryAdapter(List<Booking> bookingList, OnBookingClickListener listener) {
        this.bookingList = bookingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        holder.bind(booking, listener);
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvKosName, tvKosAddress, tvStatus, tvBookingDate, tvPrice;
        Button btnAction, btnReview;
        View layoutActions;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKosName = itemView.findViewById(R.id.tvKosName);
            tvKosAddress = itemView.findViewById(R.id.tvKosAddress);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvBookingDate = itemView.findViewById(R.id.tvBookingDate);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnAction = itemView.findViewById(R.id.btnAction);
            btnReview = itemView.findViewById(R.id.btnReview);
            layoutActions = itemView.findViewById(R.id.layoutActions);
        }

        void bind(Booking booking, OnBookingClickListener listener) {
            tvKosName.setText(booking.getKosName());
            tvKosAddress.setText(booking.getKosAddress());
            tvBookingDate.setText(DateHelper.formatDate(booking.getBookingDate()));
            
            double pricePerMonth = booking.getDurationMonth() > 0 ? 
                    booking.getTotalPrice() / booking.getDurationMonth() : booking.getTotalPrice();
            tvPrice.setText(CurrencyHelper.formatRupiah(pricePerMonth) + "/bln");

            setStatusBadge(booking.getStatus());
            setupActionButton(booking, listener);

            itemView.setOnClickListener(v -> listener.onItemClick(booking));
            btnReview.setOnClickListener(v -> listener.onReviewClick(booking));
            btnAction.setOnClickListener(v -> listener.onActionClick(booking));
        }

        private void setStatusBadge(String statusParam) {
            Context context = itemView.getContext();
            int bgColor, textColor;
            String statusText;

            String status = statusParam != null ? statusParam : "";

            switch (status) {
                case DatabaseConstants.BOOKING_PENDING:
                    bgColor = R.color.status_pending_bg;
                    textColor = R.color.status_pending_text;
                    statusText = "Menunggu";
                    break;
                case DatabaseConstants.BOOKING_ACCEPTED:
                    bgColor = R.color.status_active_bg;
                    textColor = R.color.status_active_text;
                    statusText = "Diterima";
                    break;
                case DatabaseConstants.BOOKING_WAITING_PAYMENT:
                    bgColor = R.color.status_warning_bg;
                    textColor = R.color.status_warning_text;
                    statusText = "Menunggu Bayar";
                    break;
                case DatabaseConstants.BOOKING_ACTIVE:
                    bgColor = R.color.status_accepted_bg;
                    textColor = R.color.status_accepted_text;
                    statusText = "Aktif";
                    break;
                case DatabaseConstants.BOOKING_COMPLETED:
                    bgColor = R.color.status_completed_bg;
                    textColor = R.color.status_completed_text;
                    statusText = "Selesai";
                    break;
                case DatabaseConstants.BOOKING_CANCELLED:
                case DatabaseConstants.BOOKING_REJECTED:
                    bgColor = R.color.status_rejected_bg;
                    textColor = R.color.status_rejected_text;
                    statusText = status.equals(DatabaseConstants.BOOKING_CANCELLED) ? "Dibatalkan" : "Ditolak";
                    break;
                case DatabaseConstants.BOOKING_WAITING_CHECKIN:
                    bgColor = R.color.status_info_bg;
                    textColor = R.color.status_info_text;
                    statusText = "Siap Check-in";
                    break;
                default:
                    bgColor = R.color.border_soft;
                    textColor = R.color.text_secondary;
                    statusText = status;
                    break;
            }

            tvStatus.setText(statusText);
            tvStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, bgColor)));
            tvStatus.setTextColor(ContextCompat.getColor(context, textColor));
        }

        private void setupActionButton(Booking booking, OnBookingClickListener listener) {
            String status = booking.getStatus() != null ? booking.getStatus() : "";
            
            // Primary action logic
            if (DatabaseConstants.BOOKING_ACCEPTED.equals(status) || DatabaseConstants.BOOKING_WAITING_PAYMENT.equals(status)) {
                if (DatabaseConstants.PAYMENT_PENDING.equals(booking.getPaymentStatus())) {
                    btnAction.setText("Lanjutkan");
                } else {
                    btnAction.setText("Bayar");
                }
                btnAction.setVisibility(View.VISIBLE);
            } else if (DatabaseConstants.BOOKING_ACTIVE.equals(status) || DatabaseConstants.BOOKING_WAITING_CHECKIN.equals(status)) {
                btnAction.setText("Chat");
                btnAction.setVisibility(View.VISIBLE);
            } else {
                btnAction.setVisibility(View.GONE);
            }
            
            // Review button is always visible as requested
            btnReview.setVisibility(View.VISIBLE);
        }
    }
}
