package com.koshub.psdku.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.koshub.psdku.R;
import com.koshub.psdku.models.Chat;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private final List<Chat> chatList;
    private final OnChatClickListener listener;
    private final String currentUserId;
    private final String userRole;

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    public ChatListAdapter(List<Chat> chatList, String currentUserId, String userRole, OnChatClickListener listener) {
        this.chatList = chatList;
        this.currentUserId = currentUserId;
        this.userRole = userRole;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_conversation, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        holder.bind(chat, currentUserId, userRole, listener);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatarInitial, tvUserName, tvChatTime, tvKosKamar, tvLastMessage, tvUnreadBadge, tvBookingStatus;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvChatTime = itemView.findViewById(R.id.tvChatTime);
            tvKosKamar = itemView.findViewById(R.id.tvKosKamar);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
            tvBookingStatus = itemView.findViewById(R.id.tvBookingStatus);
        }

        public void bind(Chat chat, String currentUserId, String userRole, OnChatClickListener listener) {
            boolean isOwner = DatabaseConstants.ROLE_OWNER.equals(userRole);
            String opponentName = isOwner ? chat.getStudentName() : chat.getOwnerName();
            
            tvUserName.setText(opponentName != null ? opponentName : "User");
            tvAvatarInitial.setText(opponentName != null && !opponentName.isEmpty() ? opponentName.substring(0, 1).toUpperCase() : "?");
            
            tvChatTime.setText(formatTime(chat.getLastMessageAt()));
            tvKosKamar.setText(chat.getKosName());
            tvLastMessage.setText(chat.getLastMessage());
            
            int unreadCount = isOwner ? chat.getOwnerUnreadCount() : chat.getStudentUnreadCount();
            if (unreadCount > 0) {
                tvUnreadBadge.setVisibility(View.VISIBLE);
                tvUnreadBadge.setText(String.valueOf(unreadCount));
            } else {
                tvUnreadBadge.setVisibility(View.GONE);
            }

            // Status is optional, could be based on booking if available
            tvBookingStatus.setVisibility(View.GONE); // Default hide for now unless we add logic

            itemView.setOnClickListener(v -> listener.onChatClick(chat));
        }

        private String formatTime(long timestamp) {
            if (timestamp == 0) return "";
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(timestamp);
            
            Calendar now = Calendar.getInstance();
            if (now.get(Calendar.DATE) == cal.get(Calendar.DATE)) {
                return DateFormat.format("HH:mm", cal).toString();
            } else {
                return DateFormat.format("dd/MM", cal).toString();
            }
        }
    }
}
