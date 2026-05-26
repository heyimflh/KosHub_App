package com.koshub.psdku;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.models.Review;
import com.koshub.psdku.repositories.ReviewRepository;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;

public class ReviewFormActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private EditText etComment;
    private View btnSubmit;
    private TextView tvKosName;
    private String bookingId;
    private Booking currentBooking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_form);

        bookingId = getIntent().getStringExtra("BOOKING_ID");
        if (bookingId == null) {
            Toast.makeText(this, "ID Booking tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadBookingData();
    }

    private void initViews() {
        ratingBar = findViewById(R.id.ratingBar);
        etComment = findViewById(R.id.etComment);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvKosName = findViewById(R.id.tvKosName);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitReview());
    }

    private void loadBookingData() {
        FirebaseService.getFirestore().collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentBooking = documentSnapshot.toObject(Booking.class);
                    if (currentBooking != null) {
                        tvKosName.setText(currentBooking.getKosName());
                        checkEligibility();
                    }
                });
    }

    private void checkEligibility() {
        ReviewRepository.getInstance().canReviewBooking(bookingId, new ReviewRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                btnSubmit.setEnabled(true);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ReviewFormActivity.this, message, Toast.LENGTH_LONG).show();
                btnSubmit.setEnabled(false);
            }
        });
    }

    private void submitReview() {
        float rating = ratingBar.getRating();
        String comment = etComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Rating wajib dipilih", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comment.length() < 5) {
            Toast.makeText(this, "Komentar minimal 5 karakter", Toast.LENGTH_SHORT).show();
            return;
        }

        Review review = new Review();
        review.setBookingId(bookingId);
        review.setKosId(currentBooking.getKosId());
        review.setKosName(currentBooking.getKosName());
        review.setRating(rating);
        review.setComment(comment);
        
        // Student name from booking or Auth
        review.setStudentName(currentBooking.getStudentName());

        btnSubmit.setEnabled(false);
        ReviewRepository.getInstance().createReview(review, new ReviewRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(ReviewFormActivity.this, "Review berhasil dikirim.", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                btnSubmit.setEnabled(true);
                Toast.makeText(ReviewFormActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
