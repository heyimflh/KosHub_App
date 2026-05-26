package com.koshub.psdku;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.repositories.ComplaintRepository;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.List;

public class TenantComplaintFormActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spinnerCategory;
    private EditText etTitle, etDesc;
    private Button btnSubmit;
    private LinearLayout btnUploadPhoto;
    private ImageView imgPreview;
    private TextView tvKosName, tvRoomInfo;

    private String bookingId;
    private Booking selectedBooking;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imgPreview.setVisibility(View.VISIBLE);
                    Glide.with(this).load(uri).into(imgPreview);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_complaint_form);

        bookingId = getIntent().getStringExtra("bookingId");

        initViews();
        setupCategorySpinner();
        setupListeners();
        loadBookingData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackTenantComplaint);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        etTitle = findViewById(R.id.etComplaintTitle);
        etDesc = findViewById(R.id.etComplaintDesc);
        btnSubmit = findViewById(R.id.btnSubmitComplaint);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        imgPreview = findViewById(R.id.imgComplaintPreview);
        tvKosName = findViewById(R.id.tvComplaintKosName);
        tvRoomInfo = findViewById(R.id.tvComplaintRoomInfo);
    }

    private void setupCategorySpinner() {
        String[] categories = {"Fasilitas", "Kebersihan", "Internet", "Pembayaran", "Keamanan", "Lainnya"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> NavigationTransitionHelper.finishWithBackTransition(this));

        btnUploadPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                submitComplaint();
            }
        });
    }

    private void loadBookingData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            showToast("Sesi berakhir, silakan login kembali");
            finish();
            return;
        }

        BookingRepository.getInstance().getBookingsByStudent(uid, new BookingRepository.BookingListCallback() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                List<Booking> activeBookings = new ArrayList<>();
                for (Booking b : bookings) {
                    if (DatabaseConstants.BOOKING_ACTIVE.equals(b.getStatus())) {
                        activeBookings.add(b);
                    }
                }

                if (activeBookings.isEmpty()) {
                    showToast("Kamu tidak memiliki sewa aktif untuk dilaporkan");
                    finish();
                    return;
                }

                // If bookingId passed, find it. Otherwise pick the first active one.
                if (bookingId != null) {
                    for (Booking b : activeBookings) {
                        if (bookingId.equals(b.getId())) {
                            selectedBooking = b;
                            break;
                        }
                    }
                }

                if (selectedBooking == null) {
                    selectedBooking = activeBookings.get(0);
                    bookingId = selectedBooking.getId();
                }

                displayBookingInfo();
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat data sewa: " + message);
                finish();
            }
        });
    }

    private void displayBookingInfo() {
        if (selectedBooking != null) {
            tvKosName.setText(selectedBooking.getKosName());
            tvRoomInfo.setText("Kamar " + selectedBooking.getRoomName() + " • Aktif Ngekos");
        }
    }

    private void submitComplaint() {
        String title = "[" + spinnerCategory.getSelectedItem().toString() + "] " + etTitle.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Memeriksa...");

        // Prevention of duplicate active complaints
        ComplaintRepository.getInstance().getComplaintsByStudent(FirebaseAuth.getInstance().getUid(), new ComplaintRepository.ComplaintListCallback() {
            @Override
            public void onSuccess(List<com.koshub.psdku.models.Complaint> complaints) {
                for (com.koshub.psdku.models.Complaint c : complaints) {
                    if (bookingId.equals(c.getBookingId()) && 
                        (DatabaseConstants.COMPLAINT_NEW.equals(c.getStatus()) || DatabaseConstants.COMPLAINT_PROCESS.equals(c.getStatus()))) {
                        showToast("Kamu masih memiliki komplain aktif untuk sewa ini.");
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Kirim Laporan");
                        return;
                    }
                }
                
                // No active complaint, proceed to create
                btnSubmit.setText("Mengirim...");
                ComplaintRepository.getInstance().createComplaintFromBooking(
                        TenantComplaintFormActivity.this, bookingId, title, desc, selectedImageUri, new ComplaintRepository.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                showToast("Laporan komplain berhasil dikirim");
                                NavigationTransitionHelper.finishWithBackTransition(TenantComplaintFormActivity.this);
                            }

                            @Override
                            public void onError(String message) {
                                showToast(message);
                                btnSubmit.setEnabled(true);
                                btnSubmit.setText("Kirim Laporan");
                            }
                        }
                );
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memvalidasi komplain: " + message);
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Kirim Laporan");
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavigationTransitionHelper.finishWithBackTransition(this);
    }

    private boolean validateForm() {
        if (etTitle.getText().toString().trim().length() < 3) {
            showToast("Judul komplain minimal 3 karakter");
            return false;
        }
        if (etDesc.getText().toString().trim().length() < 10) {
            showToast("Deskripsi komplain minimal 10 karakter");
            return false;
        }
        if (bookingId == null) {
            showToast("Data sewa tidak valid");
            return false;
        }
        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
