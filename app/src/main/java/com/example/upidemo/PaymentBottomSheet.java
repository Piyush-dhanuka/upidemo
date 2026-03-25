package com.example.upidemo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.UUID;

public class PaymentBottomSheet extends BottomSheetDialogFragment {
    private String groupId;
    private double initialAmount;
    private TextInputEditText amountInput;
    private Button confirmBtn;
    private FirebaseFirestore db;
    private PrefManager pref;

    public static PaymentBottomSheet newInstance(String groupId, double amount) {
        PaymentBottomSheet fragment = new PaymentBottomSheet();
        Bundle args = new Bundle();
        args.putString("groupId", groupId);
        args.putDouble("amount", amount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            groupId = getArguments().getString("groupId");
            initialAmount = getArguments().getDouble("amount");
        }
        db = FirebaseFirestore.getInstance();
        pref = new PrefManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_payment, container, false);
        
        amountInput = view.findViewById(R.id.amountInput);
        confirmBtn = view.findViewById(R.id.confirmSettleBtn);

        // Pre-fill with the amount the user owes
        amountInput.setText(String.format("%.2f", initialAmount));

        confirmBtn.setOnClickListener(v -> processSettlement());

        return view;
    }

    private void processSettlement() {
        String amtStr = amountInput.getText().toString();
        if (amtStr.isEmpty()) {
            Toast.makeText(getContext(), "Enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amtStr);
        String expenseId = UUID.randomUUID().toString();
        
        // A settlement is essentially an expense paid by the debtor to the group
        Expense settlement = new Expense(
                expenseId,
                "group",
                groupId,
                pref.getUserId(),
                amount,
                "Group Settlement",
                null, // This amount effectively reduces their debt in the next calculation
                System.currentTimeMillis()
        );

        db.collection("expenses").document(expenseId).set(settlement)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Settlement Recorded", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to record settlement", Toast.LENGTH_SHORT).show());
    }
}
