package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * OwnerChatActivity - Halaman Inbox / Daftar Chat untuk Owner
 */
public class OwnerChatActivity extends AppCompatActivity {

    private LinearLayout itemChat1, itemChat2, itemChat3, itemChat4, itemChat5;
    private TextView tabSemua, tabBelumDibaca, tabBooking, tabAktif, tabKomplain;
    private TextView tvHeaderUnreadBadge;
    private EditText etSearchChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_chat);

        initViews();
        setupChatList();
        setupFilters();
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.CHAT);
    }

    private void initViews() {
        itemChat1 = findViewById(R.id.itemChat1);
        itemChat2 = findViewById(R.id.itemChat2);
        itemChat3 = findViewById(R.id.itemChat3);
        itemChat4 = findViewById(R.id.itemChat4);
        itemChat5 = findViewById(R.id.itemChat5);

        tabSemua = findViewById(R.id.tabSemua);
        tabBelumDibaca = findViewById(R.id.tabBelumDibaca);
        tabBooking = findViewById(R.id.tabBooking);
        tabAktif = findViewById(R.id.tabAktif);
        tabKomplain = findViewById(R.id.tabKomplain);

        tvHeaderUnreadBadge = findViewById(R.id.tvHeaderUnreadBadge);
        if (tvHeaderUnreadBadge != null) {
            tvHeaderUnreadBadge.setText(getString(R.string.owner_chat_unread_count, 5));
        }

        etSearchChat = findViewById(R.id.etSearchChat);
    }

    private void setupChatList() {
        // Dummy data initialization for each item via include
        setupChatItem(itemChat1, "M", "Muhammad Fakhri", "10.24", "Kos Melati Indah • A-12", "Apakah kunci bisa diambil sore ini?", "2", "Siap Ambil Kunci");
        setupChatItem(itemChat2, "S", "Sinta Aulia", "09.10", "Kos Melati Indah • A-05", "WiFi kamar saya sering mati.", "1", "Komplain Baru");
        setupChatItem(itemChat3, "R", "Raka Pratama", "Kemarin", "Kos Mawar Residence • B-04", "Terima kasih Pak.", "0", "Aktif Ngekos");
        setupChatItem(itemChat4, "N", "Nabila Putri", "Senin", "Kos Mawar Residence • B-07", "Saya ingin survei kos.", "0", "Booking Menunggu");
        setupChatItem(itemChat5, "D", "Dimas Saputra", "Minggu", "Kos Anggrek • C-02", "Baik, terima kasih.", "0", "Selesai");

        itemChat1.setOnClickListener(v -> openChatRoom("fakhri", "Muhammad Fakhri", "Mahasiswa UNS", "Kos Melati Indah", "A-12", "Siap Ambil Kunci", "M"));
        itemChat2.setOnClickListener(v -> openChatRoom("sinta", "Sinta Aulia", "Mahasiswa Aktif", "Kos Melati Indah", "A-05", "Komplain Baru", "S"));
        itemChat3.setOnClickListener(v -> openChatRoom("raka", "Raka Pratama", "Mahasiswa Baru", "Kos Mawar Residence", "B-04", "Aktif Ngekos", "R"));
        itemChat4.setOnClickListener(v -> openChatRoom("nabila", "Nabila Putri", "Mahasiswa", "Kos Mawar Residence", "B-07", "Booking Menunggu", "N"));
        itemChat5.setOnClickListener(v -> openChatRoom("dimas", "Dimas Saputra", "Mahasiswa", "Kos Anggrek", "C-02", "Selesai", "D"));
    }

    private void setupChatItem(View itemView, String initial, String name, String time, String kos, String message, String unread, String status) {
        TextView tvAvatar = itemView.findViewById(R.id.tvAvatarInitial);
        TextView tvName = itemView.findViewById(R.id.tvUserName);
        TextView tvTime = itemView.findViewById(R.id.tvChatTime);
        TextView tvKos = itemView.findViewById(R.id.tvKosKamar);
        TextView tvMsg = itemView.findViewById(R.id.tvLastMessage);
        TextView tvBadge = itemView.findViewById(R.id.tvUnreadBadge);
        TextView tvStatus = itemView.findViewById(R.id.tvBookingStatus);

        tvAvatar.setText(initial);
        tvName.setText(name);
        tvTime.setText(time);
        tvKos.setText(kos);
        tvMsg.setText(message);

        if (unread.equals("0")) {
            tvBadge.setVisibility(View.GONE);
        } else {
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText(unread);
        }

        tvStatus.setText(status);
        
        // Soft Styling Status Badges
        if (status.equals("Komplain Baru")) {
            tvStatus.setBackgroundResource(R.drawable.bg_badge_rejected);
            tvStatus.setTextColor(getResources().getColor(R.color.status_rejected_text));
        } else if (status.equals("Selesai")) {
            tvStatus.setBackgroundResource(R.drawable.bg_status_completed_bg);
            tvStatus.setTextColor(getResources().getColor(R.color.status_completed_text));
        } else if (status.equals("Aktif Ngekos")) {
            tvStatus.setBackgroundResource(R.drawable.bg_badge_accepted);
            tvStatus.setTextColor(getResources().getColor(R.color.status_accepted_text));
        } else if (status.equals("Siap Ambil Kunci")) {
            tvStatus.setBackgroundResource(R.drawable.bg_status_info_bg);
            tvStatus.setTextColor(getResources().getColor(R.color.status_info_text));
        } else if (status.equals("Booking Menunggu")) {
            tvStatus.setBackgroundResource(R.drawable.bg_badge_pending);
            tvStatus.setTextColor(getResources().getColor(R.color.status_pending_text));
        }
    }

    private void setupFilters() {
        tabSemua.setOnClickListener(v -> selectTab(tabSemua, getString(R.string.owner_chat_tab_semua)));
        tabBelumDibaca.setOnClickListener(v -> selectTab(tabBelumDibaca, getString(R.string.owner_chat_tab_unread)));
        tabBooking.setOnClickListener(v -> selectTab(tabBooking, getString(R.string.owner_chat_tab_booking)));
        tabAktif.setOnClickListener(v -> selectTab(tabAktif, getString(R.string.owner_chat_tab_aktif)));
        tabKomplain.setOnClickListener(v -> selectTab(tabKomplain, getString(R.string.owner_chat_tab_komplain)));
    }

    private void selectTab(TextView selectedTab, String filterName) {
        // Reset all tabs
        TextView[] tabs = {tabSemua, tabBelumDibaca, tabBooking, tabAktif, tabKomplain};
        for (TextView tab : tabs) {
            tab.setBackgroundResource(R.drawable.bg_chip_inactive_premium);
            tab.setTextColor(getResources().getColor(R.color.text_secondary));
        }

        // Set selected
        selectedTab.setBackgroundResource(R.drawable.bg_chip_active);
        selectedTab.setTextColor(getResources().getColor(R.color.white));

        Toast.makeText(this, "Menampilkan chat " + filterName, Toast.LENGTH_SHORT).show();
    }

    private void openChatRoom(String chatId, String name, String role, String kos, String kamar, String status, String initial) {
        Intent intent = new Intent(this, OwnerChatRoomActivity.class);
        intent.putExtra("CHAT_ID", chatId);
        intent.putExtra("USER_NAME", name);
        intent.putExtra("USER_ROLE", role);
        intent.putExtra("KOS_NAME", kos);
        intent.putExtra("KAMAR", kamar);
        intent.putExtra("STATUS", status);
        intent.putExtra("INITIAL", initial);
        startActivity(intent);
    }
}
