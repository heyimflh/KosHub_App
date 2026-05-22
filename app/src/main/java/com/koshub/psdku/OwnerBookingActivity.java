package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class OwnerBookingActivity extends AppCompatActivity {

    private LinearLayout bookingListContainer;
    private TextView tabAll, tabPending, tabAccepted, tabActive, tabCompleted, tabRejected;
    private View btnFilter, btnPriorityBooking, btnSeeAllList, btnNotification;
    private EditText etSearch;

    private List<BookingItem> allBookings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_booking);

        initViews();
        setupDummyData();
        setupClickListeners();
        renderBookings("Menunggu"); // Default active tab
    }

    private void initViews() {
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.BOOKING);

        bookingListContainer = findViewById(R.id.bookingListContainer);

        tabAll = findViewById(R.id.tabAll);
        tabPending = findViewById(R.id.tabPending);
        tabAccepted = findViewById(R.id.tabAccepted);
        tabActive = findViewById(R.id.tabActive);
        tabCompleted = findViewById(R.id.tabCompleted);
        tabRejected = findViewById(R.id.tabRejected);

        btnFilter = findViewById(R.id.btnFilter);
        btnPriorityBooking = findViewById(R.id.btnPriorityBooking);
        btnSeeAllList = findViewById(R.id.btnSeeAllList);
        btnNotification = findViewById(R.id.btnNotification);
        etSearch = findViewById(R.id.etSearch);
    }

    private void setupDummyData() {
        allBookings.clear();
        // Synchronized: 3 Pending bookings as per Summary and Alert
        allBookings.add(new BookingItem("Muhammad Fakhri", "Mahasiswa UNS", "Kos Melati Indah", "A-12", "20 Mei 2026", "22 Mei 2026", "1 bulan", "Rp 850.000", "Menunggu"));
        allBookings.add(new BookingItem("Ahmad Subarjo", "Karyawan", "Kos Melati Indah", "B-02", "20 Mei 2026", "23 Mei 2026", "1 bulan", "Rp 850.000", "Menunggu"));
        allBookings.add(new BookingItem("Siti Aminah", "Mahasiswi", "Kos Melati Indah", "A-07", "20 Mei 2026", "25 Mei 2026", "1 bulan", "Rp 850.000", "Menunggu"));
        
        allBookings.add(new BookingItem("Raka Pratama", "Mahasiswa Baru", "Kos Mawar Residence", "B-04", "19 Mei 2026", "25 Mei 2026", "3 bulan", "Rp 900.000", "Siap Check-in"));
        allBookings.add(new BookingItem("Sinta Aulia", "Mahasiswa Aktif", "Kos Melati Indah", "A-05", "18 Mei 2026", "21 Mei 2026", "6 bulan", "Rp 850.000", "Aktif Ngekos"));
        allBookings.add(new BookingItem("Dimas Saputra", "Mahasiswa", "Kos Anggrek", "C-02", "17 Mei 2026", "24 Mei 2026", "1 bulan", "Rp 750.000", "Ditolak"));
        allBookings.add(new BookingItem("Nabila Putri", "Mahasiswa", "Kos Mawar Residence", "B-07", "15 Mei 2026", "16 Mei 2026", "1 bulan", "Rp 900.000", "Selesai"));
    }

    private void setupClickListeners() {
        // Tabs
        tabAll.setOnClickListener(v -> selectTab(tabAll, "Semua"));
        tabPending.setOnClickListener(v -> selectTab(tabPending, "Menunggu"));
        tabAccepted.setOnClickListener(v -> selectTab(tabAccepted, "Diterima"));
        tabActive.setOnClickListener(v -> selectTab(tabActive, "Aktif"));
        tabCompleted.setOnClickListener(v -> selectTab(tabCompleted, "Selesai"));
        tabRejected.setOnClickListener(v -> selectTab(tabRejected, "Ditolak"));

        // Others
        btnFilter.setOnClickListener(v -> showToast("Filter booking belum tersedia"));
        btnPriorityBooking.setOnClickListener(v -> renderBookings("Menunggu"));
        btnSeeAllList.setOnClickListener(v -> selectTab(tabAll, "Semua"));
        btnNotification.setOnClickListener(v -> showToast("Membuka notifikasi"));
    }

    private void selectTab(TextView selectedTab, String status) {
        // Reset all tabs
        TextView[] tabs = {tabAll, tabPending, tabAccepted, tabActive, tabCompleted, tabRejected};
        for (TextView tab : tabs) {
            tab.setBackgroundResource(R.drawable.bg_chip_inactive_premium);
            tab.setTextColor(getResources().getColor(R.color.text_secondary));
            tab.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // Highlight selected
        selectedTab.setBackgroundResource(R.drawable.bg_chip_active);
        selectedTab.setTextColor(getResources().getColor(R.color.text_white));
        selectedTab.setTypeface(null, android.graphics.Typeface.BOLD);

        showToast("Menampilkan booking " + status);
        renderBookings(status);
    }

    private void renderBookings(String filterStatus) {
        bookingListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (BookingItem item : allBookings) {
            boolean matches = filterStatus.equals("Semua") 
                || item.status.equals(filterStatus)
                || (filterStatus.equals("Aktif") && (item.status.equals("Siap Check-in") || item.status.equals("Aktif Ngekos")))
                || (filterStatus.equals("Diterima") && item.status.equals("Siap Check-in"));

            if (matches) {
                View itemView = inflater.inflate(R.layout.item_owner_booking, bookingListContainer, false);

                TextView tvInitial = itemView.findViewById(R.id.tvInitial);
                TextView tvName = itemView.findViewById(R.id.tvTenantName);
                TextView tvStatus = itemView.findViewById(R.id.tvTenantStatus);
                TextView tvBookingStatus = itemView.findViewById(R.id.tvBookingStatus);
                TextView tvKosRoom = itemView.findViewById(R.id.tvKosName);
                TextView tvCheckIn = itemView.findViewById(R.id.tvCheckInDate);
                TextView tvDuration = itemView.findViewById(R.id.tvDuration);
                TextView tvPrice = itemView.findViewById(R.id.tvPrice);

                View btnAccept = itemView.findViewById(R.id.btnAccept);
                View btnReject = itemView.findViewById(R.id.btnReject);
                View btnDetail = itemView.findViewById(R.id.btnDetail);
                View btnChat = itemView.findViewById(R.id.btnChat);
                View btnViewReason = itemView.findViewById(R.id.btnViewReason);

                tvInitial.setText(item.name.substring(0, 1));
                tvName.setText(item.name);
                tvStatus.setText(item.tenantStatus);
                tvBookingStatus.setText(item.status);
                tvKosRoom.setText(item.kosName + " • " + item.roomNo);
                tvCheckIn.setText(item.checkInDate);
                tvDuration.setText(item.duration);
                tvPrice.setText(item.price);

                // Status Styling
                if (item.status.equals("Menunggu")) {
                    tvBookingStatus.setBackgroundResource(R.drawable.bg_badge_pending);
                    tvBookingStatus.setTextColor(getResources().getColor(R.color.status_pending_text));
                    btnAccept.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.VISIBLE);
                    btnDetail.setVisibility(View.VISIBLE);
                    btnChat.setVisibility(View.GONE);
                    btnViewReason.setVisibility(View.GONE);
                } else if (item.status.equals("Diterima") || item.status.equals("Siap Check-in")) {
                    tvBookingStatus.setBackgroundResource(R.drawable.bg_status_info_bg); // Blue soft for check-in
                    tvBookingStatus.setTextColor(getResources().getColor(R.color.status_info_text));
                    if (item.status.equals("Diterima")) {
                        tvBookingStatus.setBackgroundResource(R.drawable.bg_badge_accepted);
                        tvBookingStatus.setTextColor(getResources().getColor(R.color.status_accepted_text));
                    }
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    btnDetail.setVisibility(View.VISIBLE);
                    btnChat.setVisibility(View.VISIBLE);
                    btnViewReason.setVisibility(View.GONE);
                } else if (item.status.equals("Aktif Ngekos")) {
                    tvBookingStatus.setBackgroundResource(R.drawable.bg_badge_accepted);
                    tvBookingStatus.setTextColor(getResources().getColor(R.color.status_accepted_text));
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    btnDetail.setVisibility(View.VISIBLE);
                    btnChat.setVisibility(View.VISIBLE);
                    btnViewReason.setVisibility(View.GONE);
                } else if (item.status.equals("Selesai")) {
                    tvBookingStatus.setBackgroundResource(R.drawable.bg_status_completed_bg);
                    tvBookingStatus.setTextColor(getResources().getColor(R.color.status_completed_text));
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    btnDetail.setVisibility(View.VISIBLE);
                    btnChat.setVisibility(View.GONE);
                    btnViewReason.setVisibility(View.GONE);
                } else if (item.status.equals("Ditolak")) {
                    tvBookingStatus.setBackgroundResource(R.drawable.bg_badge_rejected);
                    tvBookingStatus.setTextColor(getResources().getColor(R.color.status_rejected_text));
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    btnDetail.setVisibility(View.GONE);
                    btnChat.setVisibility(View.GONE);
                    btnViewReason.setVisibility(View.VISIBLE);
                }

                btnAccept.setOnClickListener(v -> showToast("Booking diterima"));
                btnReject.setOnClickListener(v -> showToast("Booking ditolak"));
                btnChat.setOnClickListener(v -> {
                    NavigationTransitionHelper.navigateDetail(this, OwnerChatActivity.class);
                });
                btnViewReason.setOnClickListener(v -> showToast("Alasan: Dokumen tidak lengkap"));
                btnDetail.setOnClickListener(v -> showBookingDetailDialog(item));

                bookingListContainer.addView(itemView);
            }
        }
    }

    private void showBookingDetailDialog(BookingItem item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_booking_detail, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog).setView(dialogView).create();

        TextView tvName = dialogView.findViewById(R.id.tvDetailTenantName);
        TextView tvKos = dialogView.findViewById(R.id.tvDetailKosName);
        TextView tvRoom = dialogView.findViewById(R.id.tvDetailRoom);
        TextView tvCheckIn = dialogView.findViewById(R.id.tvDetailCheckIn);
        TextView tvDuration = dialogView.findViewById(R.id.tvDetailDuration);
        TextView tvTotal = dialogView.findViewById(R.id.tvDetailTotal);
        
        View btnAccept = dialogView.findViewById(R.id.btnDialogAccept);
        View btnChat = dialogView.findViewById(R.id.btnDialogChat);
        View btnClose = dialogView.findViewById(R.id.btnDialogClose);

        tvName.setText(item.name);
        tvKos.setText(item.kosName);
        tvRoom.setText("Nomor Kamar: " + item.roomNo);
        tvCheckIn.setText(item.checkInDate);
        tvDuration.setText(item.duration);
        tvTotal.setText(item.price);

        if (item.status.equals("Menunggu")) {
            btnAccept.setVisibility(View.VISIBLE);
        }

        btnAccept.setOnClickListener(v -> {
            showToast("Booking diterima");
            dialog.dismiss();
        });
        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerChatActivity.class);
            startActivity(intent);
            dialog.dismiss();
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Inner class for dummy data
    private static class BookingItem {
        String name, tenantStatus, kosName, roomNo, bookingDate, checkInDate, duration, price, status;

        BookingItem(String name, String tenantStatus, String kosName, String roomNo, String bookingDate, String checkInDate, String duration, String price, String status) {
            this.name = name;
            this.tenantStatus = tenantStatus;
            this.kosName = kosName;
            this.roomNo = roomNo;
            this.bookingDate = bookingDate;
            this.checkInDate = checkInDate;
            this.duration = duration;
            this.price = price;
            this.status = status;
        }
    }
}
