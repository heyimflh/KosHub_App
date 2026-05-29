package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.koshub.psdku.adapters.BookingHistoryAdapter;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.repositories.ChatRepository;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.List;

public class RentalHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private BookingHistoryAdapter adapter;
    private final List<Booking> allBookings = new ArrayList<>();
    private final List<Booking> filteredBookings = new ArrayList<>();
    private ProgressBar progressBar;
    private View layoutEmpty;
    private ChipGroup chipGroupFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rental_history);

        initViews();
        setupListeners();
        loadBookings();
    }

    private void initViews() {
        rvHistory = findViewById(R.id.rvHistory);
        progressBar = findViewById(R.id.progressBar);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingHistoryAdapter(filteredBookings, new BookingHistoryAdapter.OnBookingClickListener() {
            @Override
            public void onActionClick(Booking booking) {
                handleAction(booking);
            }

            @Override
            public void onReviewClick(Booking booking) {
                openReviewForm(booking);
            }

            @Override
            public void onItemClick(Booking booking) {
                // Potential future detail view
            }
        });
        rvHistory.setAdapter(adapter);
    }

    private void setupListeners() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> filterBookings(checkedId));
    }

    private void loadBookings() {
        String studentId = FirebaseAuth.getInstance().getUid();
        if (studentId == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        BookingRepository.getInstance().getBookingsByStudent(studentId, new BookingRepository.BookingListCallback() {
            @Override
            public void onSuccess(List<Booking> list) {
                progressBar.setVisibility(View.GONE);
                allBookings.clear();
                allBookings.addAll(list);
                filterBookings(chipGroupFilter.getCheckedChipId());
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RentalHistoryActivity.this, error, Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void filterBookings(int checkedId) {
        filteredBookings.clear();
        if (checkedId == R.id.chipAll) {
            filteredBookings.addAll(allBookings);
        } else if (checkedId == R.id.chipActive) {
            for (Booking b : allBookings) {
                if (DatabaseConstants.BOOKING_ACTIVE.equals(b.getStatus()) || 
                    DatabaseConstants.BOOKING_WAITING_CHECKIN.equals(b.getStatus())) {
                    filteredBookings.add(b);
                }
            }
        } else if (checkedId == R.id.chipCompleted) {
            for (Booking b : allBookings) {
                if (DatabaseConstants.BOOKING_COMPLETED.equals(b.getStatus())) {
                    filteredBookings.add(b);
                }
            }
        } else if (checkedId == R.id.chipCancelled) {
            for (Booking b : allBookings) {
                if (DatabaseConstants.BOOKING_CANCELLED.equals(b.getStatus()) || 
                    DatabaseConstants.BOOKING_REJECTED.equals(b.getStatus())) {
                    filteredBookings.add(b);
                }
            }
        } else if (checkedId == R.id.chipPending) {
            for (Booking b : allBookings) {
                if (DatabaseConstants.BOOKING_PENDING.equals(b.getStatus()) || 
                    DatabaseConstants.BOOKING_ACCEPTED.equals(b.getStatus())) {
                    filteredBookings.add(b);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredBookings.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
        }
    }

    private void handleAction(Booking booking) {
        String status = booking.getStatus();
        if (DatabaseConstants.BOOKING_ACCEPTED.equals(status)) {
            simulatePayment(booking);
        } else if (DatabaseConstants.BOOKING_ACTIVE.equals(status) || DatabaseConstants.BOOKING_WAITING_CHECKIN.equals(status)) {
            openChat(booking);
        }
    }

    private void openReviewForm(Booking booking) {
        Intent intent = new Intent(this, ReviewFormActivity.class);
        intent.putExtra("BOOKING_ID", booking.getId());
        intent.putExtra("KOS_ID", booking.getKosId());
        intent.putExtra("KOS_NAME", booking.getKosName());
        startActivity(intent);
    }

    private void simulatePayment(Booking booking) {
        progressBar.setVisibility(View.VISIBLE);
        BookingRepository.getInstance().simulatePayment(booking.getId(), new BookingRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RentalHistoryActivity.this, "Pembayaran berhasil!", Toast.LENGTH_SHORT).show();
                loadBookings();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RentalHistoryActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openChat(Booking booking) {
        progressBar.setVisibility(View.VISIBLE);
        ChatRepository.getInstance().getOrCreateChatFromBooking(booking.getId(), new ChatRepository.ChatCallback() {
            @Override
            public void onSuccess(com.koshub.psdku.models.Chat chat) {
                progressBar.setVisibility(View.GONE);
                Intent intent = new Intent(RentalHistoryActivity.this, OwnerChatRoomActivity.class);
                intent.putExtra("CHAT_ID", chat.getId());
                intent.putExtra("USER_NAME", chat.getOwnerName());
                intent.putExtra("KOS_NAME", chat.getKosName());
                startActivity(intent);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RentalHistoryActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
