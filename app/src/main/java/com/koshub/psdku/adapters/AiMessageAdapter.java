package com.koshub.psdku.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.koshub.psdku.R;
import com.koshub.psdku.models.AiMessage;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AiMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;

    private final List<AiMessage> messages;
    private OnFeedbackListener feedbackListener;

    public interface OnFeedbackListener {
        void onNotHelpful(int position);
    }

    public void setFeedbackListener(OnFeedbackListener feedbackListener) {
        this.feedbackListener = feedbackListener;
    }

    public AiMessageAdapter(List<AiMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        if ("user".equals(messages.get(position).getSenderType())) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_AI;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_message_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_message_bot, parent, false);
            return new AiViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AiMessage message = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(message);
        } else {
            ((AiViewHolder) holder).bind(message, feedbackListener);
            android.util.Log.d("AiMessageAdapter", "AI message length displayed: " + message.getMessage().length());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageUser);
            tvTime = itemView.findViewById(R.id.tvTimeUser);
        }

        public void bind(AiMessage message) {
            tvMessage.setText(message.getMessage());
            tvTime.setText(formatTime(message.getTimestamp()));

            itemView.setOnLongClickListener(v -> {
                copyToClipboard(v.getContext(), message.getMessage(), "Pesan disalin");
                return true;
            });
        }
    }

    static class AiViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvNotHelpful;

        public AiViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageBot);
            tvTime = itemView.findViewById(R.id.tvTimeBot);
            tvNotHelpful = itemView.findViewById(R.id.tvNotHelpful);
        }

        public void bind(AiMessage message, OnFeedbackListener listener) {
            tvMessage.setText(message.getMessage());
            tvTime.setText(formatTime(message.getTimestamp()));

            // Declutter: Remove "Tidak membantu?" logic for a cleaner UI
            if (tvNotHelpful != null) {
                tvNotHelpful.setVisibility(View.GONE);
            }

            boolean isTyping = "typing".equals(message.getId());

            itemView.setOnLongClickListener(v -> {
                if (!isTyping) {
                    copyToClipboard(v.getContext(), message.getMessage(), "Jawaban disalin");
                    return true;
                }
                return false;
            });
        }
    }

    private static void copyToClipboard(Context context, String text, String toastMessage) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("KosHub Chat", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private static String formatTime(long timestamp) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp);
        return DateFormat.format("HH:mm", cal).toString();
    }
}
