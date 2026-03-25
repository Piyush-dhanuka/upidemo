package com.example.upidemo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private RecyclerView expensesRv;
    private ExpenseAdapter adapter;
    private List<Expense> expenseList;
    private TextView youOweText, youAreOwedText;
    private Map<String, String> userNames = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ledger);

        db = FirebaseFirestore.getInstance();
        pref = new PrefManager(this);
        groupId = getIntent().getStringExtra("groupId");

        expensesRv = findViewById(R.id.expensesRv);
        youOweText = findViewById(R.id.youOweText);
        youAreOwedText = findViewById(R.id.youAreOwedText);

        expenseList = new ArrayList<>();
        adapter = new ExpenseAdapter(expenseList, userNames);
        expensesRv.setLayoutManager(new LinearLayoutManager(this));
        expensesRv.setAdapter(adapter);

        loadUserNames();
    }

    private void loadUserNames() {
        db.collection("users").get().addOnSuccessListener(querySnapshot -> {
            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                User user = doc.toObject(User.class);
                userNames.put(user.userId, user.name);
            }
            loadExpenses();
        });
    }

    private void loadExpenses() {
        db.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        expenseList.clear();
                        double totalOwe = 0;
                        double totalOwed = 0;
                        String currentUserId = pref.getUserId();

                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Expense expense = doc.toObject(Expense.class);
                            if (expense != null) {
                                expenseList.add(expense);

                                if (expense.splitBetween != null && expense.splitBetween.contains(currentUserId)) {
                                    double splitAmount = expense.amount / expense.splitBetween.size();
                                    if (expense.paidBy.equals(currentUserId)) {
                                        totalOwed += (expense.amount - splitAmount);
                                    } else {
                                        totalOwe += splitAmount;
                                    }
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        youOweText.setText("You owe: ₹" + String.format("%.2f", totalOwe));
                        youAreOwedText.setText("You are owed: ₹" + String.format("%.2f", totalOwed));
                    }
                });
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
