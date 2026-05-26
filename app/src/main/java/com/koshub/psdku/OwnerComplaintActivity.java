package com.koshub.psdku;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.koshub.psdku.models.Complaint;
import com.koshub.psdku.repositories.ComplaintRepository;
import com.koshub.psdku.utils.DatabaseConstants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OwnerComplaintActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout complaintListContainer;
    private List<Complaint> complaints = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_complaint);

        initViews();
        loadComplaints();
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.NONE);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackComplaint);
        complaintListContainer = findViewById(R.id.complaintListContainer);
        btnBack.setOnClickListener(v -> NavigationTransitionHelper.finishWithBackTransition(this));
    }

    private void loadComplaints() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        ComplaintRepository.getInstance().getComplaintsByOwner(uid, new ComplaintRepository.ComplaintListCallback() {
            @Override
            public void onSuccess(List<Complaint> result) {
                complaints = result;
                renderComplaints();
            }

            @Override
            public void onError(String message) {
                showToast(message);
            }
        });
    }

    private void renderComplaints() {
        complaintListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (complaints.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Belum ada komplain masuk.");
            tvEmpty.setPadding(32, 32, 32, 32);
            tvEmpty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            complaintListContainer.addView(tvEmpty);
            return;
        }

        for (Complaint item : complaints) {
            View itemView = inflater.inflate(R.layout.item_owner_complaint, complaintListContainer, false);

            TextView tvCategory = itemView.findViewById(R.id.tvComplaintCategory);
            TextView tvStatus = itemView.findViewById(R.id.tvComplaintStatus);
            TextView tvTitle = itemView.findViewById(R.id.tvComplaintTitle);
            TextView tvTenant = itemView.findViewById(R.id.tvComplaintTenant);
            TextView tvDate = itemView.findViewById(R.id.tvComplaintDate);
            Button btnProcess = itemView.findViewById(R.id.btnProcessComplaint);
            Button btnComplete = itemView.findViewById(R.id.btnCompleteComplaint);

            tvCategory.setText(item.getKosName());
            tvTitle.setText(item.getTitle());
            tvTenant.setText("Oleh: " + item.getStudentName() + " (" + item.getRoomName() + ")");
            tvDate.setText(sdf.format(new Date(item.getCreatedAt())));

            updateStatusUI(tvStatus, item.getStatus());

            if (DatabaseConstants.COMPLAINT_DONE.equals(item.getStatus()) || DatabaseConstants.COMPLAINT_REJECTED.equals(item.getStatus())) {
                btnProcess.setVisibility(View.GONE);
                btnComplete.setVisibility(View.GONE);
            } else if (DatabaseConstants.COMPLAINT_PROCESS.equals(item.getStatus())) {
                btnProcess.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> showComplaintDetail(item));

            btnProcess.setOnClickListener(v -> ComplaintRepository.getInstance().markComplaintProcess(item.getId(), new ComplaintRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    showToast("Komplain sedang diproses");
                    loadComplaints();
                }

                @Override
                public void onError(String message) {
                    showToast(message);
                }
            }));

            btnComplete.setOnClickListener(v -> showOwnerResponseDialog(item, DatabaseConstants.COMPLAINT_DONE));

            complaintListContainer.addView(itemView);
        }
    }

    private void updateStatusUI(TextView tvStatus, String status) {
        tvStatus.setText(status.toUpperCase());
        switch (status) {
            case DatabaseConstants.COMPLAINT_DONE:
                tvStatus.setBackgroundResource(R.drawable.bg_finance_status_success);
                tvStatus.setTextColor(getResources().getColor(R.color.finance_income_green));
                break;
            case DatabaseConstants.COMPLAINT_PROCESS:
                tvStatus.setBackgroundResource(R.drawable.bg_finance_status_pending);
                tvStatus.setTextColor(getResources().getColor(R.color.finance_pending_orange));
                break;
            case DatabaseConstants.COMPLAINT_REJECTED:
                tvStatus.setBackgroundResource(R.drawable.bg_badge_rejected);
                tvStatus.setTextColor(getResources().getColor(R.color.finance_expense_red));
                break;
            default:
                tvStatus.setBackgroundResource(R.drawable.bg_finance_status_pending);
                tvStatus.setTextColor(getResources().getColor(R.color.text_secondary));
                break;
        }
    }

    private void showComplaintDetail(Complaint c) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);

        TextView tvInfo = new TextView(this);
        tvInfo.setText("Pelapor: " + c.getStudentName() + "\nKos: " + c.getKosName() + " (" + c.getRoomName() + ")\n\n" + c.getDescription());
        tvInfo.setTextSize(14);
        layout.addView(tvInfo);

        if (c.getImageUrl() != null && !c.getImageUrl().isEmpty()) {
            ImageView img = new ImageView(this);
            layout.addView(img);
            img.getLayoutParams().height = 600;
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Glide.with(this).load(c.getImageUrl()).into(img);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(c.getTitle())
                .setView(layout)
                .setPositiveButton("Tutup", null);

        if (!DatabaseConstants.COMPLAINT_DONE.equals(c.getStatus()) && !DatabaseConstants.COMPLAINT_REJECTED.equals(c.getStatus())) {
            builder.setNeutralButton("Tolak", (dialog, which) -> showOwnerResponseDialog(c, DatabaseConstants.COMPLAINT_REJECTED));
        }

        builder.show();
    }

    private void showOwnerResponseDialog(Complaint c, String targetStatus) {
        EditText et = new EditText(this);
        et.setHint("Tambahkan pesan untuk penyewa (opsional)...");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        et.setLayoutParams(lp);

        LinearLayout container = new LinearLayout(this);
        container.setPadding(48, 24, 48, 24);
        container.addView(et);

        String actionTitle = DatabaseConstants.COMPLAINT_DONE.equals(targetStatus) ? "Selesaikan Komplain" : "Tolak Komplain";

        new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(actionTitle)
                .setView(container)
                .setPositiveButton("Kirim", (dialog, which) -> {
                    String response = et.getText().toString().trim();
                    ComplaintRepository.getInstance().updateComplaintStatus(c.getId(), targetStatus, response, new ComplaintRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("Komplain diperbarui");
                            loadComplaints();
                        }

                        @Override
                        public void onError(String message) {
                            showToast(message);
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavigationTransitionHelper.finishWithBackTransition(this);
    }
}
