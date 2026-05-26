package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.koshub.psdku.models.AppNotification;
import com.koshub.psdku.repositories.NotificationRepository;
import com.koshub.psdku.utils.DatabaseConstants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private TextView btnReadAll;
    private ImageView btnBack;

    private NotificationAdapter adapter;
    private List<AppNotification> notificationList = new ArrayList<>();
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        initViews();
        setupRecyclerView();
        loadNotifications();
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        btnReadAll = findViewById(R.id.btnReadAll);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnReadAll.setOnClickListener(v -> markAllAsRead());
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.bg_divider_line)); // Correct divider drawable
        rvNotifications.addItemDecoration(divider);
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        listenerRegistration = NotificationRepository.getInstance().listenNotificationsForCurrentUser(new NotificationRepository.NotificationListCallback() {
            @Override
            public void onSuccess(List<AppNotification> notifications) {
                progressBar.setVisibility(View.GONE);
                notificationList.clear();
                notificationList.addAll(notifications);
                adapter.notifyDataSetChanged();

                if (notificationList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    rvNotifications.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    rvNotifications.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NotificationActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAllAsRead() {
        NotificationRepository.getInstance().markAllAsRead(new NotificationRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                // UI will update automatically via listener
            }

            @Override
            public void onError(String message) {
                Toast.makeText(NotificationActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    private void handleNotificationClick(AppNotification notif) {
        if (!notif.isRead()) {
            NotificationRepository.getInstance().markAsRead(notif.getId(), null);
        }

        Intent intent;
        String type = notif.getTargetType();
        String id = notif.getTargetId();

        if (DatabaseConstants.TARGET_OWNER_BOOKING.equals(type)) {
            intent = new Intent(this, OwnerBookingActivity.class);
        } else if (DatabaseConstants.TARGET_WAITING_LIST.equals(type)) {
            intent = new Intent(this, WaitingListQueueActivity.class);
            intent.putExtra("BOOKING_ID", id);
        } else if (DatabaseConstants.TARGET_CHAT.equals(type)) {
            intent = new Intent(this, OwnerChatRoomActivity.class);
            intent.putExtra("CHAT_ID", id);
        } else if (DatabaseConstants.TARGET_FINANCE.equals(type) || DatabaseConstants.TARGET_WITHDRAW.equals(type)) {
            intent = new Intent(this, OwnerFinanceReportActivity.class);
        } else {
            // Default to home or just close if no target
            return;
        }

        NavigationTransitionHelper.navigateDetailWithIntent(this, intent);
    }

    // --- ADAPTER ---
    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private final List<AppNotification> list;
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());

        public NotificationAdapter(List<AppNotification> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppNotification item = list.get(position);
            holder.tvTitle.setText(item.getTitle());
            holder.tvBody.setText(item.getBody());
            holder.tvTime.setText(sdf.format(new Date(item.getCreatedAt())));
            holder.viewUnread.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);

            // Icon logic
            int iconRes = R.drawable.ic_bell;
            if (DatabaseConstants.TARGET_CHAT.equals(item.getTargetType())) iconRes = R.drawable.ic_owner_chat;
            else if (DatabaseConstants.TARGET_OWNER_BOOKING.equals(item.getTargetType())) iconRes = R.drawable.ic_owner_booking;
            else if (DatabaseConstants.TARGET_COMPLAINT.equals(item.getTargetType())) iconRes = R.drawable.ic_owner_warning;
            else if (DatabaseConstants.TARGET_FINANCE.equals(item.getTargetType())) iconRes = R.drawable.ic_owner_wallet;
            
            holder.ivIcon.setImageResource(iconRes);
            holder.itemView.setOnClickListener(v -> handleNotificationClick(item));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvBody, tvTime;
            ImageView ivIcon;
            View viewUnread;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvNotifTitle);
                tvBody = itemView.findViewById(R.id.tvNotifBody);
                tvTime = itemView.findViewById(R.id.tvNotifTime);
                ivIcon = itemView.findViewById(R.id.ivNotifIcon);
                viewUnread = itemView.findViewById(R.id.viewUnreadDot);
            }
        }
    }
}
