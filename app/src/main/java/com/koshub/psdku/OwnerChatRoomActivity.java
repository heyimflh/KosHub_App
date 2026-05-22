package com.koshub.psdku;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * OwnerChatRoomActivity - Improved dynamic detail conversation room.
 */
public class OwnerChatRoomActivity extends AppCompatActivity {

    private ImageView btnBackChat, btnSendMessage, btnAttach, btnMoreOptions;
    private TextView tvRoomUserName, tvRoomUserStatus, tvRoomAvatarInitial;
    private TextView tvContextTitle, tvContextSubtitle, tvContextStatus;
    private TextView btnTemplatePesan, btnTerimaBooking, btnTolakBooking, btnViewBookingDetail;
    private EditText etMessage;
    private LinearLayout messageContainer;
    private ScrollView scrollChat;

    private String chatId, tenantName, tenantRole, kosName, roomNumber, status, initial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_chat_room);

        initViews();
        handleIntentData();
        setupListeners();
    }

    private void initViews() {
        btnBackChat = findViewById(R.id.btnBackChat);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnAttach = findViewById(R.id.btnAttach);
        btnMoreOptions = findViewById(R.id.btnMoreOptions);

        tvRoomUserName = findViewById(R.id.tvRoomUserName);
        tvRoomUserStatus = findViewById(R.id.tvRoomUserStatus);
        tvRoomAvatarInitial = findViewById(R.id.tvRoomAvatarInitial);
        
        tvContextTitle = findViewById(R.id.tvContextTitle);
        tvContextSubtitle = findViewById(R.id.tvContextSubtitle);
        tvContextStatus = findViewById(R.id.tvContextStatus);

        btnTemplatePesan = findViewById(R.id.btnTemplatePesan);
        btnTerimaBooking = findViewById(R.id.btnTerimaBooking);
        btnTolakBooking = findViewById(R.id.btnTolakBooking);
        btnViewBookingDetail = findViewById(R.id.btnViewBookingDetail);

        etMessage = findViewById(R.id.etMessage);
        messageContainer = findViewById(R.id.messageContainer);
        scrollChat = findViewById(R.id.scrollChat);
    }

    private void handleIntentData() {
        chatId = getIntent().getStringExtra("CHAT_ID");
        tenantName = getIntent().getStringExtra("USER_NAME");
        tenantRole = getIntent().getStringExtra("USER_ROLE");
        kosName = getIntent().getStringExtra("KOS_NAME");
        roomNumber = getIntent().getStringExtra("KAMAR");
        status = getIntent().getStringExtra("STATUS");
        initial = getIntent().getStringExtra("INITIAL");

        // Fallback for direct testing
        if (chatId == null) chatId = "fakhri";
        if (tenantName == null) tenantName = "Muhammad Fakhri";
        if (tenantRole == null) tenantRole = "Mahasiswa UNS";
        if (kosName == null) kosName = "Kos Melati Indah";
        if (roomNumber == null) roomNumber = "A-12";
        if (status == null) status = "Siap Ambil Kunci";
        if (initial == null) initial = "M";

        populateHeader();
        populateContextCard();
        loadConversationByChatId(chatId);
        updateQuickActions();
    }

    private void populateHeader() {
        tvRoomUserName.setText(tenantName);
        tvRoomAvatarInitial.setText(initial);
        
        String onlineStatus;
        switch (chatId) {
            case "raka":
                onlineStatus = "Terakhir dilihat kemarin";
                break;
            case "dimas":
                onlineStatus = "Offline";
                break;
            default:
                onlineStatus = "Online";
                break;
        }
        
        tvRoomUserStatus.setText(String.format("%s • %s", tenantRole, onlineStatus));
    }

    private void populateContextCard() {
        tvContextTitle.setText(kosName);
        tvContextStatus.setText(status);
        
        // Status Badge Styling
        switch (status) {
            case "Komplain Baru":
                tvContextStatus.setBackgroundResource(R.drawable.bg_badge_rejected);
                tvContextStatus.setTextColor(ContextCompat.getColor(this, R.color.status_rejected_text));
                tvContextTitle.setText("Komplain: WiFi kamar sering mati");
                tvContextSubtitle.setText(String.format("%s • Kamar %s", kosName, roomNumber));
                btnViewBookingDetail.setText("Detail Komplain");
                break;
            case "Selesai":
                tvContextStatus.setBackgroundResource(R.drawable.bg_status_completed_bg);
                tvContextStatus.setTextColor(ContextCompat.getColor(this, R.color.status_completed_text));
                tvContextSubtitle.setText(String.format("Kamar %s • Rp 750.000/bulan", roomNumber));
                break;
            case "Aktif Ngekos":
                tvContextStatus.setBackgroundResource(R.drawable.bg_badge_accepted);
                tvContextStatus.setTextColor(ContextCompat.getColor(this, R.color.status_accepted_text));
                tvContextSubtitle.setText(String.format("Kamar %s • Mulai: 25 Mei 2026", roomNumber));
                break;
            case "Siap Ambil Kunci":
                tvContextStatus.setBackgroundResource(R.drawable.bg_status_info_bg);
                tvContextStatus.setTextColor(ContextCompat.getColor(this, R.color.status_info_text));
                tvContextSubtitle.setText(String.format("Kamar %s • Masuk: 22 Mei 2026", roomNumber));
                break;
            default:
                tvContextStatus.setBackgroundResource(R.drawable.bg_badge_pending);
                tvContextStatus.setTextColor(ContextCompat.getColor(this, R.color.status_pending_text));
                tvContextSubtitle.setText(String.format("Kamar %s • Rp 900.000/bulan", roomNumber));
                break;
        }
    }

    private void updateQuickActions() {
        // Clear specific ones and show based on status
        btnTerimaBooking.setVisibility(View.GONE);
        btnTolakBooking.setVisibility(View.GONE);

        switch (status) {
            case "Booking Menunggu":
                btnTerimaBooking.setVisibility(View.VISIBLE);
                btnTerimaBooking.setText("Terima Booking");
                btnTolakBooking.setVisibility(View.VISIBLE);
                btnTolakBooking.setText("Tolak Booking");
                break;
            case "Komplain Baru":
                btnTerimaBooking.setVisibility(View.VISIBLE);
                btnTerimaBooking.setText("Tandai Diproses");
                btnTolakBooking.setVisibility(View.VISIBLE);
                btnTolakBooking.setText("Hubungi Penyewa");
                btnTolakBooking.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
                break;
            case "Siap Ambil Kunci":
                btnTerimaBooking.setVisibility(View.VISIBLE);
                btnTerimaBooking.setText("Ingatkan Ambil Kunci");
                break;
            case "Aktif Ngekos":
                btnTerimaBooking.setVisibility(View.VISIBLE);
                btnTerimaBooking.setText("Detail Penyewa");
                btnTolakBooking.setVisibility(View.VISIBLE);
                btnTolakBooking.setText("Lihat Komplain");
                btnTolakBooking.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
                break;
            case "Selesai":
                btnTerimaBooking.setVisibility(View.VISIBLE);
                btnTerimaBooking.setText("Detail");
                break;
        }
    }

    private void loadConversationByChatId(String chatId) {
        messageContainer.removeAllViews();
        addDateSeparator("Hari ini");

        switch (chatId) {
            case "fakhri":
                addMessageBubble("Halo Pak, kamar A-12 masih tersedia?", "10.20", false);
                addMessageBubble("Halo, masih tersedia ya. Kamar A-12 sudah siap ditempati.", "10.22", true);
                addMessageBubble("Apakah kunci bisa diambil sore ini?", "10.24", false);
                break;
            case "sinta":
                addMessageBubble("Pak, saya ingin lapor kendala.", "09.00", false);
                addMessageBubble("Baik, kendalanya apa ya?", "09.02", true);
                addMessageBubble("WiFi kamar saya sering mati.", "09.05", false);
                addMessageBubble("Usually mati malam hari dan sulit dipakai untuk kuliah online.", "09.06", false);
                addMessageBubble("Baik, saya cek ke teknisi hari ini ya.", "09.10", true);
                break;
            case "raka":
                addMessageBubble("Pak, saya sudah masuk kamar B-04.", "Yesterday", false);
                addMessageBubble("Baik, semoga nyaman ya. Kalau ada kendala langsung kabari.", "Yesterday", true);
                addMessageBubble("Terima kasih Pak.", "Yesterday", false);
                break;
            case "nabila":
                addMessageBubble("Halo Pak, apakah kamar B-07 masih tersedia?", "08.00", false);
                addMessageBubble("Halo, masih tersedia.", "08.15", true);
                addMessageBubble("Saya ingin survei kos.", "08.30", false);
                addMessageBubble("Bisa, mau survei hari apa?", "08.45", true);
                break;
            case "dimas":
                addMessageBubble("Terima kasih atas bantuannya Pak.", "Sunday", false);
                addMessageBubble("Sama-sama, semoga betah selama tinggal di kos.", "Sunday", true);
                addMessageBubble("Baik, terima kasih.", "Sunday", false);
                break;
        }
    }

    private void addMessageBubble(String message, String time, boolean isOwner) {
        int layout = isOwner ? R.layout.item_chat_message_right : R.layout.item_chat_message_left;
        View messageView = LayoutInflater.from(this).inflate(layout, messageContainer, false);
        
        TextView tvMsg = messageView.findViewById(isOwner ? R.id.tvMessageRight : R.id.tvMessageLeft);
        TextView tvTime = messageView.findViewById(isOwner ? R.id.tvTimeRight : R.id.tvTimeLeft);

        tvMsg.setText(message);
        tvTime.setText(time);

        messageContainer.addView(messageView);
    }

    private void addDateSeparator(String date) {
        View separator = LayoutInflater.from(this).inflate(R.layout.include_chat_date_separator, messageContainer, false);
        TextView tvDate = separator.findViewById(R.id.tvChatDate);
        tvDate.setText(date);
        messageContainer.addView(separator);
    }

    private void setupListeners() {
        btnBackChat.setOnClickListener(v -> finish());

        btnMoreOptions.setOnClickListener(v ->
                Toast.makeText(this, "Opsi chat belum tersedia", Toast.LENGTH_SHORT).show());

        btnViewBookingDetail.setOnClickListener(v ->
                Toast.makeText(this, "Membuka " + btnViewBookingDetail.getText(), Toast.LENGTH_SHORT).show());

        btnTerimaBooking.setOnClickListener(v ->
                Toast.makeText(this, "Aksi: " + btnTerimaBooking.getText(), Toast.LENGTH_SHORT).show());

        btnTolakBooking.setOnClickListener(v ->
                Toast.makeText(this, "Aksi: " + btnTolakBooking.getText(), Toast.LENGTH_SHORT).show());

        btnAttach.setOnClickListener(v ->
                Toast.makeText(this, "Upload bukti/foto belum tersedia", Toast.LENGTH_SHORT).show());

        btnTemplatePesan.setOnClickListener(v -> showTemplateDialog());

        btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void showTemplateDialog() {
        String[] templates = {
                "Kamar masih tersedia",
                "Silakan datang cek lokasi",
                "Pembayaran bisa dilakukan lewat aplikasi",
                "Kunci bisa diambil sore ini",
                "Mohon tunggu, komplain sedang diproses"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pilih Template Pesan");
        builder.setItems(templates, (dialog, which) -> {
            etMessage.setText(templates[which]);
            etMessage.setSelection(etMessage.getText().length());
        });
        builder.show();
    }

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Pesan tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        addMessageBubble(message, "10.25", true);
        etMessage.setText("");

        // Scroll to bottom
        scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
    }
}
