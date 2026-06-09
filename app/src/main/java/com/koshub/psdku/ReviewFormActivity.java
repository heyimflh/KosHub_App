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
    private String kosId, kosName, reviewId;
    private long existingCreatedAt = 0;
    private Booking currentBooking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_form);

        bookingId = getIntent().getStringExtra("BOOKING_ID");
        kosId = getIntent().getStringExtra("KOS_ID");
        kosName = getIntent().getStringExtra("KOS_NAME");
        reviewId = getIntent().getStringExtra("REVIEW_ID");

        if (bookingId == null && reviewId == null) {
            Toast.makeText(this, "Review harus dibuat dari booking yang valid.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        
        if (bookingId != null) {
            loadBookingData();
        } else if (reviewId != null) {
            // If editing existing review, we might need booking info from it or just allow edit
            loadExistingReview();
        }
    }

    private void loadExistingReview() {
        FirebaseService.getFirestore().collection(DatabaseConstants.COLLECTION_REVIEWS).document(reviewId).get()
                .addOnSuccessListener(doc -> {
                    Review r = doc.toObject(Review.class);
                    if (r != null) {
                        bookingId = r.getBookingId();
                        kosId = r.getKosId();
                        kosName = r.getKosName();
                        existingCreatedAt = r.getCreatedAt();
                        
                        tvKosName.setText(kosName);
                        ratingBar.setRating((float) r.getRating());
                        etComment.setText(r.getComment());
                        ((TextView)btnSubmit).setText("Update Ulasan");
                        btnSubmit.setEnabled(true);
                    }
                });
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

        if (bookingId == null || bookingId.isEmpty()) {
            Toast.makeText(this, "Review harus dibuat dari booking yang valid.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentBooking == null && reviewId == null) {
            Toast.makeText(this, "Data booking belum siap.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rating == 0) {
            Toast.makeText(this, "Rating wajib dipilih", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comment.length() < 5) {
            Toast.makeText(this, "Komentar minimal 5 karakter", Toast.LENGTH_SHORT).show();
            return;
        }

        Review review = new Review();
        if (reviewId != null) {
            review.setId(reviewId);
            review.setCreatedAt(existingCreatedAt);
        }
        review.setBookingId(bookingId);
        
        if (currentBooking != null) {
            review.setKosId(currentBooking.getKosId());
            review.setKosName(currentBooking.getKosName());
            review.setStudentName(currentBooking.getStudentName());
        } else {
            review.setKosId(kosId);
            review.setKosName(kosName);
            // studentName will be set in repository from Auth
        }
        
        review.setRating(rating);
        review.setComment(comment);

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
