package com.example.upidemo;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.UUID;

public class ProfileFragment extends Fragment {
    private FirebaseFirestore db;
    private PrefManager pref;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        db = FirebaseFirestore.getInstance();
        pref = new PrefManager(requireContext());

        TextView userName = view.findViewById(R.id.userName);
        TextView userId = view.findViewById(R.id.userId);
        Button reviewBtn = view.findViewById(R.id.reviewBtn);
        Button logoutBtn = view.findViewById(R.id.logoutBtn);

        userName.setText(pref.getUserName());
        userId.setText("ID: " + pref.getUserId());

        reviewBtn.setOnClickListener(v -> showReviewDialog());

        logoutBtn.setOnClickListener(v -> {
            pref.logout();
            startActivity(new Intent(requireContext(), SignupActivity.class));
            requireActivity().finish();
        });

        return view;
    }

    private void showReviewDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_review, null);
        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        EditText reviewInput = view.findViewById(R.id.reviewInput);

        new AlertDialog.Builder(requireContext())
                .setView(view)
                .setPositiveButton("Submit", (dialog, which) -> {
                    float rating = ratingBar.getRating();
                    String comment = reviewInput.getText().toString().trim();
                    if (rating > 0) {
                        saveReview(rating, comment);
                    } else {
                        Toast.makeText(requireContext(), "Please select a rating", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveReview(float rating, String comment) {
        String reviewId = UUID.randomUUID().toString();
        Review review = new Review(
                reviewId,
                pref.getUserId(),
                pref.getUserName(),
                rating,
                comment,
                System.currentTimeMillis()
        );

        db.collection("reviews").document(reviewId).set(review)
                .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Thank you for your review!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to submit review", Toast.LENGTH_SHORT).show());
    }
}
