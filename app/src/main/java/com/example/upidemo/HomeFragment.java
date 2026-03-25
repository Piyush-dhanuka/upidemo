package com.example.upidemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeFragment extends Fragment {
    private TextView merchantText, statusText;
    private EditText amountInput;
    private Button scanBtn, payBtn;
    private String merchantName = "";
    private FirebaseFirestore db;
    private PrefManager pref;

    private final ActivityResultLauncher<Intent> scannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    merchantName = result.getData().getStringExtra("scanned_data");
                    merchantText.setText("Merchant: " + merchantName);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        pref = new PrefManager(requireContext());

        merchantText = view.findViewById(R.id.merchantText);
        amountInput = view.findViewById(R.id.amountInput);
        statusText = view.findViewById(R.id.statusText);
        scanBtn = view.findViewById(R.id.scanBtn);
        payBtn = view.findViewById(R.id.payBtn);

        scanBtn.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CameraScannerActivity.class);
            scannerLauncher.launch(intent);
        });

        payBtn.setOnClickListener(v -> processPayment());

        return view;
    }

    private void processPayment() {
        String amtStr = amountInput.getText().toString();
        if (amtStr.isEmpty() || merchantName.isEmpty()) {
            Toast.makeText(getContext(), "Scan QR and enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amtStr);
        String expenseId = UUID.randomUUID().toString();
        
        Expense expense = new Expense(
                expenseId,
                "individual",
                null,
                pref.getUserId(),
                amount,
                merchantName,
                null,
                System.currentTimeMillis()
        );

        db.collection("expenses").document(expenseId).set(expense)
                .addOnSuccessListener(aVoid -> {
                    statusText.setText("Payment Successful: ₹" + amount);
                    amountInput.setText("");
                    merchantName = "";
                    merchantText.setText("No merchant selected");
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Payment Failed", Toast.LENGTH_SHORT).show());
    }
}
