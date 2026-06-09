package com.koshub.psdku;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.koshub.psdku.models.StudentDocument;
import com.koshub.psdku.repositories.StudentDocumentRepository;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentDocumentsActivity extends AppCompatActivity {

    private String currentUserId;
    private ListenerRegistration docListener;
    private Map<String, StudentDocument> uploadedDocs = new HashMap<>();

    private ProgressBar progressDocs;
    private TextView tvProgressTitle, tvProgressSub;
    private LinearLayout layoutCards;

    private String pendingUploadType, pendingUploadTitle;

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && pendingUploadType != null) {
                    uploadFile(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_documents);

        currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            finish();
            return;
        }

        initViews();
        applyStudentDocumentsInsets();
        listenToDocuments();
    }

    private void applyStudentDocumentsInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        View root = findViewById(R.id.rootStudentDocuments);
        View statusBarSpacer = findViewById(R.id.statusBarSpacer);
        View scroll = findViewById(R.id.scrollDocuments);

        final int scrollLeft = scroll.getPaddingLeft();
        final int scrollTop = scroll.getPaddingTop();
        final int scrollRight = scroll.getPaddingRight();
        final int scrollBottom = scroll.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.LayoutParams spacerParams = statusBarSpacer.getLayoutParams();
            if (spacerParams.height != bars.top) {
                spacerParams.height = bars.top;
                statusBarSpacer.setLayoutParams(spacerParams);
            }

            scroll.setPadding(
                    scrollLeft,
                    scrollTop,
                    scrollRight,
                    scrollBottom + bars.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        progressDocs = findViewById(R.id.progressDocuments);
        tvProgressTitle = findViewById(R.id.tvProgressTitle);
        tvProgressSub = findViewById(R.id.tvProgressSub);
        layoutCards = findViewById(R.id.layoutDocumentCards);
    }

    private void listenToDocuments() {
        if (docListener != null) docListener.remove();
        docListener = StudentDocumentRepository.getInstance().listenDocuments(currentUserId, new StudentDocumentRepository.DocumentListCallback() {
            @Override
            public void onSuccess(List<StudentDocument> documents) {
                uploadedDocs.clear();
                for (StudentDocument d : documents) {
                    uploadedDocs.put(d.getType(), d);
                }
                renderDocumentCards();
                updateProgress();
            }

            @Override
            public void onError(String message) {
                if (message.contains("Akses ditolak") || message.contains("PERMISSION_DENIED")) {
                    showToast("Akses database belum diizinkan. Periksa Firestore Rules.");
                } else {
                    showToast(message);
                }
                renderDocumentCards(); // Ensure slots are shown even on error
                updateProgress();
            }
        });
    }

    private void renderDocumentCards() {
        layoutCards.removeAllViews();

        // 1. KTP (Mandatory)
        addDocumentCard(DatabaseConstants.DOC_TYPE_KTP, "Kartu Tanda Penduduk (KTP)", "Pastikan data diri terlihat jelas.");
        
        // 2. KTM (Mandatory)
        addDocumentCard(DatabaseConstants.DOC_TYPE_KTM, "Kartu Tanda Mahasiswa (KTM)", "Gunakan kartu mahasiswa yang aktif.");

        // 3. Supporting
        addDocumentCard(DatabaseConstants.DOC_TYPE_SUPPORTING, "Dokumen Pendukung", "Opsional: Surat Keterangan Mahasiswa, dll.");
    }

    private void addDocumentCard(String type, String title, String subtitle) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_student_document, layoutCards, false);
        TextView tvTitle = cardView.findViewById(R.id.tvDocTitle);
        TextView tvStatus = cardView.findViewById(R.id.tvDocStatus);
        TextView tvBadge = cardView.findViewById(R.id.tvStatusBadge);
        ImageView ivPreview = cardView.findViewById(R.id.ivPreview);
        View layoutPreview = cardView.findViewById(R.id.layoutPreview);
        TextView tvRejection = cardView.findViewById(R.id.tvRejectionReason);
        View btnUpload = cardView.findViewById(R.id.btnUpload);
        View btnDelete = cardView.findViewById(R.id.btnDelete);

        tvTitle.setText(title);
        
        StudentDocument doc = uploadedDocs.get(type);
        if (doc != null) {
            tvStatus.setText(getStatusText(doc.getStatus()));
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText(doc.getStatus().replace("_", " ").toUpperCase());
            updateBadgeStyle(tvBadge, doc.getStatus());

            if (doc.getFileUrl() != null && !doc.getFileUrl().isEmpty()) {
                layoutPreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(doc.getFileUrl()).into(ivPreview);
            }

            if (DatabaseConstants.DOC_STATUS_REJECTED.equals(doc.getStatus()) && doc.getRejectionReason() != null) {
                tvRejection.setVisibility(View.VISIBLE);
                tvRejection.setText("Alasan Penolakan: " + doc.getRejectionReason());
            }

            ((TextView)btnUpload).setText("Ganti Dokumen");
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> StudentDocumentRepository.getInstance().deleteDocument(doc.getId(), new SimpleRepoCallback()));
        } else {
            tvStatus.setText(subtitle);
            tvBadge.setVisibility(View.GONE);
            layoutPreview.setVisibility(View.GONE);
            tvRejection.setVisibility(View.GONE);
            ((TextView)btnUpload).setText("Unggah Sekarang");
            btnDelete.setVisibility(View.GONE);
        }

        btnUpload.setOnClickListener(v -> {
            pendingUploadType = type;
            pendingUploadTitle = title;
            filePickerLauncher.launch("image/*");
        });

        layoutCards.addView(cardView);
    }

    private String getStatusText(String status) {
        switch (status) {
            case DatabaseConstants.DOC_STATUS_PENDING: return "Menunggu verifikasi";
            case DatabaseConstants.DOC_STATUS_VERIFIED: return "Terverifikasi";
            case DatabaseConstants.DOC_STATUS_REJECTED: return "Ditolak, silakan unggah ulang";
            default: return "Belum diunggah";
        }
    }

    private void updateBadgeStyle(TextView badge, String status) {
        int bgColor, textColor;
        switch (status) {
            case DatabaseConstants.DOC_STATUS_VERIFIED:
                bgColor = R.color.status_accepted_bg;
                textColor = R.color.status_accepted_text;
                break;
            case DatabaseConstants.DOC_STATUS_PENDING:
                bgColor = R.color.status_pending_bg;
                textColor = R.color.status_pending_text;
                break;
            case DatabaseConstants.DOC_STATUS_REJECTED:
                bgColor = R.color.status_rejected_bg;
                textColor = R.color.status_rejected_text;
                break;
            default:
                bgColor = R.color.border_soft;
                textColor = R.color.text_secondary;
        }
        badge.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_badge_light_gray)); // Fallback shape
        badge.setBackgroundTintList(ContextCompat.getColorStateList(this, bgColor));
        badge.setTextColor(ContextCompat.getColor(this, textColor));
    }

    private void updateProgress() {
        int count = 0;
        if (uploadedDocs.containsKey(DatabaseConstants.DOC_TYPE_KTP)) count++;
        if (uploadedDocs.containsKey(DatabaseConstants.DOC_TYPE_KTM)) count++;

        progressDocs.setProgress(count);
        tvProgressTitle.setText(count + "/2 Dokumen Wajib");

        boolean allVerified = true;
        if (count < 2) allVerified = false;
        else {
            for (StudentDocument d : uploadedDocs.values()) {
                if (DatabaseConstants.DOC_TYPE_KTP.equals(d.getType()) || DatabaseConstants.DOC_TYPE_KTM.equals(d.getType())) {
                    if (!DatabaseConstants.DOC_STATUS_VERIFIED.equals(d.getStatus())) {
                        allVerified = false;
                        break;
                    }
                }
            }
        }

        if (allVerified) {
            tvProgressSub.setText("Identitas Terverifikasi");
            tvProgressSub.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
        } else if (count > 0) {
            tvProgressSub.setText("Menunggu Verifikasi");
            tvProgressSub.setTextColor(ContextCompat.getColor(this, R.color.status_pending_text));
        } else {
            tvProgressSub.setText("Belum ada dokumen yang diunggah");
            tvProgressSub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void uploadFile(Uri uri) {
        showToast("Mengunggah " + pendingUploadTitle + "...");
        StudentDocumentRepository.getInstance().uploadDocument(this, uri, pendingUploadType, pendingUploadTitle, new StudentDocumentRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> showToast(pendingUploadTitle + " berhasil diunggah"));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showToast("Gagal unggah: " + message));
            }
        });
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private class SimpleRepoCallback implements StudentDocumentRepository.SimpleCallback {
        @Override public void onSuccess() {}
        @Override public void onError(String message) { showToast(message); }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (docListener != null) docListener.remove();
    }
}
