package com.example.upidemo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettlementCalculator {

    public static class UserStatus {
        public String userId;
        public String name;
        public double amountPaid;
        public double share;
        public double balance;
        public String statusMessage;

        public UserStatus(String userId, String name, double amountPaid, double share) {
            this.userId = userId;
            this.name = name;
            this.amountPaid = amountPaid;
            this.share = share;
            this.balance = amountPaid - share;

            if (this.balance > 0.01) {
                this.statusMessage = "The group owes you ₹" + String.format("%.2f", balance);
            } else if (this.balance < -0.01) {
                this.statusMessage = "You owe the group ₹" + String.format("%.2f", Math.abs(balance));
            } else {
                this.statusMessage = "You are settled";
            }
        }
    }

    public static class GroupSummary {
        public double totalExpense;
        public double sharePerPerson;
        public List<UserStatus> userStatuses;

        public GroupSummary(double totalExpense, double sharePerPerson, List<UserStatus> userStatuses) {
            this.totalExpense = totalExpense;
            this.sharePerPerson = sharePerPerson;
            this.userStatuses = userStatuses;
        }
    }

    public static GroupSummary calculateGroupSummary(List<User> groupMembers, List<Expense> expenses) {
        int N = groupMembers.size();
        if (N == 0) return new GroupSummary(0, 0, new ArrayList<>());

        // 1. Calculate total amount paid by each user
        Map<String, Double> paidMap = new HashMap<>();
        for (User u : groupMembers) {
            paidMap.put(u.userId, 0.0);
        }

        double totalExpense = 0;
        for (Expense e : expenses) {
            totalExpense += e.amount;
            paidMap.put(e.paidBy, paidMap.getOrDefault(e.paidBy, 0.0) + e.amount);
        }

        // 2. Calculate fair share
        double sharePerPerson = totalExpense / N;

        // 3. Create status for each user
        List<UserStatus> userStatuses = new ArrayList<>();
        for (User u : groupMembers) {
            userStatuses.add(new UserStatus(u.userId, u.name, paidMap.get(u.userId), sharePerPerson));
        }

        return new GroupSummary(totalExpense, sharePerPerson, userStatuses);
    }
}
