package com.example.upidemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupDetailActivity extends AppCompatActivity {
    private String groupId, groupName;
    private List<String> memberIds = new ArrayList<>();
    private String merchantName = "";
    private FirebaseFirestore db;
    private PrefManager pref;
    private TextView groupNameHeader, membersListText, groupMerchantText;
    private EditText groupAmountInput;

    private final ActivityResultLauncher<Intent> scannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    merchantName = result.getData().getStringExtra("scanned_data");
                    groupMerchantText.setText("Merchant: " + merchantName);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        db = FirebaseFirestore.getInstance();
        pref = new PrefManager(this);

        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");

        groupNameHeader = findViewById(R.id.groupNameHeader);
        membersListText = findViewById(R.id.membersListText);
        groupMerchantText = findViewById(R.id.groupMerchantText);
        groupAmountInput = findViewById(R.id.groupAmountInput);
        Button groupScanBtn = findViewById(R.id.groupScanBtn);
        Button groupPayBtn = findViewById(R.id.groupPayBtn);
        Button viewLedgerBtn = findViewById(R.id.viewLedgerBtn);

        groupNameHeader.setText(groupName);

        loadGroupDetails();

        groupScanBtn.setOnClickListener(v -> scannerLauncher.launch(new Intent(this, CameraScannerActivity.class)));

        groupPayBtn.setOnClickListener(v -> processGroupPayment());

        viewLedgerBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, LedgerActivity.class);
            intent.putExtra("groupId", groupId);
            intent.putExtra("groupName", groupName);
            startActivity(intent);
        });
    }

    private void loadGroupDetails() {
        db.collection("groups").document(groupId).get().addOnSuccessListener(doc -> {
            Group group = doc.toObject(Group.class);
            if (group != null) {
                memberIds = group.members;
                fetchMemberNames();
            }
        });
    }

    private void fetchMemberNames() {
        db.collection("users").whereIn("userId", memberIds).get().addOnSuccessListener(querySnapshot -> {
            StringBuilder names = new StringBuilder();
            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                User user = doc.toObject(User.class);
                names.append(user.name).append(", ");
            }
            if (names.length() > 2) names.setLength(names.length() - 2);
            membersListText.setText(names.toString());
        });
    }

    private void processGroupPayment() {
        String amtStr = groupAmountInput.getText().toString();
        if (amtStr.isEmpty() || merchantName.isEmpty()) {
            Toast.makeText(this, "Scan QR and enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amtStr);
        String expenseId = UUID.randomUUID().toString();

        Expense expense = new Expense(
                expenseId,
                "group",
                groupId,
                pref.getUserId(),
                amount,
                merchantName,
                memberIds,
                System.currentTimeMillis()
        );

        db.collection("expenses").document(expenseId).set(expense)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Group Payment Recorded", Toast.LENGTH_SHORT).show();
                    groupAmountInput.setText("");
                    merchantName = "";
                    groupMerchantText.setText("No merchant selected");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to record payment", Toast.LENGTH_SHORT).show());
    }
}
