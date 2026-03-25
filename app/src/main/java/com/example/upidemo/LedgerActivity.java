package com.example.upidemo;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LedgerActivity extends AppCompatActivity {
    private String groupId;
    private FirebaseFirestore db;
    private PrefManager pref;
    private RecyclerView expensesRv, memberDetailsRv;
    private ExpenseAdapter adapter;
    private MemberStatusAdapter memberAdapter;
    private List<Expense> expenseList;
    private List<User> groupMembers = new ArrayList<>();
    private List<SettlementCalculator.UserStatus> userStatuses = new ArrayList<>();
    private TextView totalExpenseText, perPersonShareText, myStatusText;
    private Button settleBtn;
    private Map<String, String> userNames = new HashMap<>();
    private double currentOwedAmount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ledger);

        db = FirebaseFirestore.getInstance();
        pref = new PrefManager(this);
        groupId = getIntent().getStringExtra("groupId");

        expensesRv = findViewById(R.id.expensesRv);
        memberDetailsRv = findViewById(R.id.memberDetailsRv);
        totalExpenseText = findViewById(R.id.totalExpenseText);
        perPersonShareText = findViewById(R.id.perPersonShareText);
        myStatusText = findViewById(R.id.myStatusText);
        settleBtn = findViewById(R.id.settleBtn);

        expenseList = new ArrayList<>();
        adapter = new ExpenseAdapter(expenseList, userNames);
        expensesRv.setLayoutManager(new LinearLayoutManager(this));
        expensesRv.setAdapter(adapter);

        memberAdapter = new MemberStatusAdapter(userStatuses);
        memberDetailsRv.setLayoutManager(new LinearLayoutManager(this));
        memberDetailsRv.setAdapter(memberAdapter);

        loadGroupDetails();

        settleBtn.setOnClickListener(v -> {
            // Open payment fragment or activity
            PaymentBottomSheet sheet = PaymentBottomSheet.newInstance(groupId, currentOwedAmount);
            sheet.show(getSupportFragmentManager(), "PaymentBottomSheet");
        });
    }

    private void loadGroupDetails() {
        db.collection("groups").document(groupId).get().addOnSuccessListener(documentSnapshot -> {
            Group group = documentSnapshot.toObject(Group.class);
            if (group != null && group.members != null) {
                db.collection("users").whereIn("userId", group.members).get().addOnSuccessListener(querySnapshot -> {
                    groupMembers.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        User user = doc.toObject(User.class);
                        groupMembers.add(user);
                        userNames.put(user.userId, user.name);
                    }
                    loadExpenses();
                });
            }
        });
    }

    private void loadExpenses() {
        db.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        expenseList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Expense expense = doc.toObject(Expense.class);
                            if (expense != null) expenseList.add(expense);
                        }

                        updateGroupSummary();
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void updateGroupSummary() {
        SettlementCalculator.GroupSummary summary = SettlementCalculator.calculateGroupSummary(groupMembers, expenseList);

        totalExpenseText.setText("Total Group Expense: ₹" + String.format("%.2f", summary.totalExpense));
        perPersonShareText.setText("Per Person Share: ₹" + String.format("%.2f", summary.sharePerPerson));

        userStatuses.clear();
        userStatuses.addAll(summary.userStatuses);
        memberAdapter.notifyDataSetChanged();

        // Find current user's status for the Settle button
        String myId = pref.getUserId();
        for (SettlementCalculator.UserStatus status : userStatuses) {
            if (status.userId.equals(myId)) {
                myStatusText.setText(status.statusMessage);
                if (status.balance < -0.01) {
                    currentOwedAmount = Math.abs(status.balance);
                    myStatusText.setTextColor(Color.parseColor("#D93025"));
                    settleBtn.setVisibility(View.VISIBLE);
                } else {
                    currentOwedAmount = 0;
                    myStatusText.setTextColor(status.balance > 0.01 ? Color.parseColor("#1E8E3E") : Color.GRAY);
                    settleBtn.setVisibility(View.GONE);
                }
                break;
            }
        }
    }

    private static class MemberStatusAdapter extends RecyclerView.Adapter<MemberStatusAdapter.ViewHolder> {
        private List<SettlementCalculator.UserStatus> statuses;

        public MemberStatusAdapter(List<SettlementCalculator.UserStatus> statuses) {
            this.statuses = statuses;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SettlementCalculator.UserStatus status = statuses.get(position);
            holder.text1.setText(status.name + " (Paid: ₹" + String.format("%.2f", status.amountPaid) + ")");
            holder.text2.setText(status.statusMessage);

            if (status.balance > 0.01) {
                holder.text2.setTextColor(Color.parseColor("#1E8E3E"));
            } else if (status.balance < -0.01) {
                holder.text2.setTextColor(Color.parseColor("#D93025"));
            } else {
                holder.text2.setTextColor(Color.GRAY);
            }
        }

        @Override
        public int getItemCount() { return statuses.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }

    private static class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {
        private List<Expense> expenses;
        private Map<String, String> userNames;

        public ExpenseAdapter(List<Expense> expenses, Map<String, String> userNames) {
            this.expenses = expenses;
            this.userNames = userNames;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Expense expense = expenses.get(position);
            holder.merchant.setText(expense.merchant);
            holder.amount.setText("₹" + expense.amount);
            String paidByName = userNames.get(expense.paidBy);
            holder.paidBy.setText("Paid by: " + (paidByName != null ? paidByName : "Unknown"));
        }

        @Override
        public int getItemCount() { return expenses.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView merchant, paidBy, amount;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                merchant = itemView.findViewById(R.id.merchantText);
                paidBy = itemView.findViewById(R.id.paidByText);
                amount = itemView.findViewById(R.id.amountText);
            }
        }
    }
}
