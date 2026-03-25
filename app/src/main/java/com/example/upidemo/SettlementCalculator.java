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

        public UserStatus(String userId, String name, double amountPaid, double share, String currentUserId) {
            this.userId = userId;
            this.name = name;
            this.amountPaid = amountPaid;
            this.share = share;
            this.balance = amountPaid - share;

            boolean isCurrentUser = userId.equals(currentUserId);
            String displayName = isCurrentUser ? "You" : name;

            if (this.balance > 0.01) {
                if (isCurrentUser) {
                    this.statusMessage = "The group owes you ₹" + String.format("%.2f", balance);
                } else {
                    this.statusMessage = "The group owes " + displayName + " ₹" + String.format("%.2f", balance);
                }
            } else if (this.balance < -0.01) {
                if (isCurrentUser) {
                    this.statusMessage = "You owe the group ₹" + String.format("%.2f", Math.abs(balance));
                } else {
                    this.statusMessage = displayName + " owes the group ₹" + String.format("%.2f", Math.abs(balance));
                }
            } else {
                this.statusMessage = displayName + (isCurrentUser ? " are settled" : " is settled");
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

    public static GroupSummary calculateGroupSummary(List<User> groupMembers, List<Expense> expenses, String currentUserId) {
        int N = groupMembers.size();
        if (N == 0) return new GroupSummary(0, 0, new ArrayList<>());

        Map<String, Double> netContributionMap = new HashMap<>();
        for (User u : groupMembers) {
            netContributionMap.put(u.userId, 0.0);
        }

        double totalGroupExpense = 0;
        for (Expense e : expenses) {
            if ("group".equals(e.type)) {
                totalGroupExpense += e.amount;
                netContributionMap.put(e.paidBy, netContributionMap.getOrDefault(e.paidBy, 0.0) + e.amount);
            } else if ("settlement".equals(e.type)) {
                // Settlement: Payer's contribution goes up, Recipient's contribution goes down (reimbursed)
                netContributionMap.put(e.paidBy, netContributionMap.getOrDefault(e.paidBy, 0.0) + e.amount);
                if (e.splitBetween != null && !e.splitBetween.isEmpty()) {
                    String recipientId = e.splitBetween.get(0);
                    if (netContributionMap.containsKey(recipientId)) {
                        netContributionMap.put(recipientId, netContributionMap.get(recipientId) - e.amount);
                    }
                }
            }
        }

        double sharePerPerson = totalGroupExpense / N;

        List<UserStatus> userStatuses = new ArrayList<>();
        for (User u : groupMembers) {
            userStatuses.add(new UserStatus(u.userId, u.name, netContributionMap.get(u.userId), sharePerPerson, currentUserId));
        }

        return new GroupSummary(totalGroupExpense, sharePerPerson, userStatuses);
    }
}
