package com.koshub.psdku.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.koshub.psdku.R;
import com.koshub.psdku.models.PaymentMethod;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.List;

public class PaymentMethodAdapter extends RecyclerView.Adapter<PaymentMethodAdapter.ViewHolder> {

    private List<PaymentMethod> methods;
    private OnMethodClickListener listener;

    public interface OnMethodClickListener {
        void onMethodClick(PaymentMethod method);
        void onMenuClick(View view, PaymentMethod method);
    }

    public PaymentMethodAdapter(List<PaymentMethod> methods, OnMethodClickListener listener) {
        this.methods = methods;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_method, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentMethod m = methods.get(position);
        holder.tvProvider.setText(m.getProviderName());
        
        String accountInfo = m.getAccountName();
        if (m.getMaskedNumber() != null && !m.getMaskedNumber().isEmpty()) {
            accountInfo += " • " + m.getMaskedNumber();
        }
        holder.tvAccount.setText(accountInfo);

        holder.tvDefaultBadge.setVisibility(m.isDefault() ? View.VISIBLE : View.GONE);

        // Icon based on type
        if (DatabaseConstants.PAYMENT_METHOD_QRIS.equals(m.getType())) {
            holder.ivIcon.setImageResource(R.drawable.ic_payment); // Should exist in res or use fallback
        } else if (DatabaseConstants.PAYMENT_METHOD_BANK.equals(m.getType())) {
            holder.ivIcon.setImageResource(R.drawable.ic_finance_wallet);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_finance_wallet);
        }

        holder.itemView.setOnClickListener(v -> listener.onMethodClick(m));
        holder.btnMenu.setOnClickListener(v -> listener.onMenuClick(v, m));
    }

    @Override
    public int getItemCount() {
        return methods.size();
    }

    public void updateList(List<PaymentMethod> newList) {
        this.methods = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvProvider, tvAccount, tvDefaultBadge;
        ImageButton btnMenu;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivMethodIcon);
            tvProvider = itemView.findViewById(R.id.tvMethodProvider);
            tvAccount = itemView.findViewById(R.id.tvMethodAccount);
            tvDefaultBadge = itemView.findViewById(R.id.tvDefaultBadge);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }
    }
}
