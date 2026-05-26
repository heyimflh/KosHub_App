package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.koshub.psdku.adapters.ChatListAdapter;
import com.koshub.psdku.models.Chat;
import com.koshub.psdku.repositories.ChatRepository;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * OwnerChatActivity - Halaman Inbox / Daftar Chat untuk Owner
 */
public class OwnerChatActivity extends AppCompatActivity implements ChatListAdapter.OnChatClickListener {

    private RecyclerView rvChatList;
    private ChatListAdapter adapter;
    private List<Chat> chats = new ArrayList<>();
    private ListenerRegistration chatListener;

    private TextView tabSemua, tabBelumDibaca, tabBooking, tabAktif, tabKomplain;
    private TextView tvHeaderUnreadBadge;
    private EditText etSearchChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_chat);

        initViews();
        setupFilters();
        setupRecyclerView();
        listenToChats();
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.CHAT);
    }

    private void initViews() {
        rvChatList = findViewById(R.id.rvChatList);
        tabSemua = findViewById(R.id.tabSemua);
        tabBelumDibaca = findViewById(R.id.tabBelumDibaca);
        tabBooking = findViewById(R.id.tabBooking);
        tabAktif = findViewById(R.id.tabAktif);
        tabKomplain = findViewById(R.id.tabKomplain);

        tvHeaderUnreadBadge = findViewById(R.id.tvHeaderUnreadBadge);
        etSearchChat = findViewById(R.id.etSearchChat);
    }

    private void setupRecyclerView() {
        String uid = FirebaseAuth.getInstance().getUid();
        adapter = new ChatListAdapter(chats, uid, DatabaseConstants.ROLE_OWNER, this);
        rvChatList.setLayoutManager(new LinearLayoutManager(this));
        rvChatList.setAdapter(adapter);
    }

    private void listenToChats() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        chatListener = ChatRepository.getInstance().listenChatsByRole(uid, DatabaseConstants.ROLE_OWNER, new ChatRepository.ChatListListener() {
            @Override
            public void onChatsUpdated(List<Chat> updatedChats) {
                chats.clear();
                chats.addAll(updatedChats);
                adapter.notifyDataSetChanged();
                updateUnreadBadge();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(OwnerChatActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUnreadBadge() {
        int totalUnread = 0;
        for (Chat c : chats) {
            totalUnread += c.getOwnerUnreadCount();
        }
        if (tvHeaderUnreadBadge != null) {
            if (totalUnread > 0) {
                tvHeaderUnreadBadge.setVisibility(View.VISIBLE);
                tvHeaderUnreadBadge.setText(totalUnread + " pesan belum dibaca");
            } else {
                tvHeaderUnreadBadge.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onChatClick(Chat chat) {
        Intent intent = new Intent(this, OwnerChatRoomActivity.class);
        intent.putExtra("CHAT_ID", chat.getId());
        intent.putExtra("USER_NAME", chat.getStudentName());
        intent.putExtra("KOS_NAME", chat.getKosName());
        // For Owner, the opponent is Student
        intent.putExtra("INITIAL", chat.getStudentName() != null && !chat.getStudentName().isEmpty() ? 
                chat.getStudentName().substring(0, 1).toUpperCase() : "?");
        NavigationTransitionHelper.navigateDetailWithIntent(this, intent);
    }

    private void setupFilters() {
        tabSemua.setOnClickListener(v -> selectTab(tabSemua, "Semua"));
        tabBelumDibaca.setOnClickListener(v -> selectTab(tabBelumDibaca, "Belum Dibaca"));
        tabBooking.setOnClickListener(v -> selectTab(tabBooking, "Booking"));
        tabAktif.setOnClickListener(v -> selectTab(tabAktif, "Aktif"));
        tabKomplain.setOnClickListener(v -> selectTab(tabKomplain, "Komplain"));
    }

    private void selectTab(TextView selectedTab, String filterName) {
        TextView[] tabs = {tabSemua, tabBelumDibaca, tabBooking, tabAktif, tabKomplain};
        for (TextView tab : tabs) {
            tab.setBackgroundResource(R.drawable.bg_chip_inactive_premium);
            tab.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        selectedTab.setBackgroundResource(R.drawable.bg_chip_active);
        selectedTab.setTextColor(getResources().getColor(R.color.white));
        
        Toast.makeText(this, "Filter " + filterName + " segera hadir", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListener != null) {
            chatListener.remove();
        }
    }
}
