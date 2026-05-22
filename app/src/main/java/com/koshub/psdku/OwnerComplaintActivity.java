package com.koshub.psdku;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class OwnerComplaintActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout complaintListContainer;
    private List<ComplaintItem> complaintItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_complaint);

        initViews();
        setupDummyData();
        renderComplaints();
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.NONE);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackComplaint);
        complaintListContainer = findViewById(R.id.complaintListContainer);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupDummyData() {
        complaintItems.add(new ComplaintItem("Muhammad Fakhri", "Kos Melati Indah", "A-12", "Fasilitas", "AC kamar tidak dingin", "20 Mei 2026", "Baru"));
        complaintItems.add(new ComplaintItem("Sinta Aulia", "Kos Melati Indah", "A-05", "Internet", "WiFi sering mati", "19 Mei 2026", "Diproses"));
        complaintItems.add(new ComplaintItem("Raka Pratama", "Kos Mawar Residence", "B-04", "Pembayaran", "Pembayaran belum terverifikasi", "18 Mei 2026", "Selesai"));
    }

    private void renderComplaints() {
        complaintListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (ComplaintItem item : complaintItems) {
            View itemView = inflater.inflate(R.layout.item_owner_complaint, complaintListContainer, false);

            TextView tvCategory = itemView.findViewById(R.id.tvComplaintCategory);
            TextView tvStatus = itemView.findViewById(R.id.tvComplaintStatus);
            TextView tvTitle = itemView.findViewById(R.id.tvComplaintTitle);
            TextView tvTenant = itemView.findViewById(R.id.tvComplaintTenant);
            TextView tvDate = itemView.findViewById(R.id.tvComplaintDate);
            Button btnProcess = itemView.findViewById(R.id.btnProcessComplaint);
            Button btnComplete = itemView.findViewById(R.id.btnCompleteComplaint);

            tvCategory.setText("Kategori: " + item.category);
            tvStatus.setText(item.status);
            tvTitle.setText(item.title);
            tvTenant.setText("Oleh: " + item.tenantName + " (" + item.roomNo + ")");
            tvDate.setText(item.date);

            if (item.status.equals("Selesai")) {
                btnProcess.setVisibility(View.GONE);
                btnComplete.setVisibility(View.GONE);
                tvStatus.setBackgroundResource(R.drawable.bg_finance_status_success);
                tvStatus.setTextColor(getResources().getColor(R.color.finance_income_green));
            } else if (item.status.equals("Diproses")) {
                btnProcess.setVisibility(View.GONE);
                tvStatus.setBackgroundResource(R.drawable.bg_finance_status_pending);
                tvStatus.setTextColor(getResources().getColor(R.color.finance_pending_orange));
            }

            btnProcess.setOnClickListener(v -> {
                item.status = "Diproses";
                showToast("Komplain sedang diproses");
                renderComplaints();
            });

            btnComplete.setOnClickListener(v -> {
                item.status = "Selesai";
                showToast("Komplain ditandai selesai");
                renderComplaints();
            });

            complaintListContainer.addView(itemView);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static class ComplaintItem {
        String tenantName, kosName, roomNo, category, title, date, status;

        ComplaintItem(String tenantName, String kosName, String roomNo, String category, String title, String date, String status) {
            this.tenantName = tenantName;
            this.kosName = kosName;
            this.roomNo = roomNo;
            this.category = category;
            this.title = title;
            this.date = date;
            this.status = status;
        }
    }
}
