package com.koshub.psdku.adapters;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.koshub.psdku.R;
import com.koshub.psdku.models.Message;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_LEFT = 1;
    private static final int VIEW_TYPE_RIGHT = 2;

    private final List<Message> messages;
    private final String currentUserId;

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_RIGHT;
        } else {
            return VIEW_TYPE_LEFT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_RIGHT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_right, parent, false);
            return new RightViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_left, parent, false);
            return new LeftViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (holder instanceof RightViewHolder) {
            ((RightViewHolder) holder).bind(message);
        } else {
            ((LeftViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class LeftViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivMessage;

        public LeftViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageLeft);
            tvTime = itemView.findViewById(R.id.tvTimeLeft);
            ivMessage = itemView.findViewById(R.id.ivMessageLeft);
        }

        public void bind(Message message) {
            if (DatabaseConstants.MESSAGE_TYPE_IMAGE.equals(message.getType())) {
                ivMessage.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(message.getImageUrl())
                        .placeholder(R.color.background_screen)
                        .into(ivMessage);
                
                if (message.getText() != null && !message.getText().trim().isEmpty()) {
                    tvMessage.setVisibility(View.VISIBLE);
                    tvMessage.setText(message.getText());
                } else {
                    tvMessage.setVisibility(View.GONE);
                }
            } else {
                tvMessage.setVisibility(View.VISIBLE);
                ivMessage.setVisibility(View.GONE);
                tvMessage.setText(message.getText());
            }
            tvTime.setText(formatTime(message.getCreatedAt()));
        }
    }

    static class RightViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivMessage;

        public RightViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageRight);
            tvTime = itemView.findViewById(R.id.tvTimeRight);
            ivMessage = itemView.findViewById(R.id.ivMessageRight);
        }

        public void bind(Message message) {
            if (DatabaseConstants.MESSAGE_TYPE_IMAGE.equals(message.getType())) {
                ivMessage.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(message.getImageUrl())
                        .placeholder(R.color.brand_green_dark)
                        .into(ivMessage);

                if (message.getText() != null && !message.getText().trim().isEmpty()) {
                    tvMessage.setVisibility(View.VISIBLE);
                    tvMessage.setText(message.getText());
                } else {
                    tvMessage.setVisibility(View.GONE);
                }
            } else {
                tvMessage.setVisibility(View.VISIBLE);
                ivMessage.setVisibility(View.GONE);
                tvMessage.setText(message.getText());
            }
            tvTime.setText(formatTime(message.getCreatedAt()));
        }
    }

    private static String formatTime(long timestamp) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp);
        return DateFormat.format("HH:mm", cal).toString();
    }
}
