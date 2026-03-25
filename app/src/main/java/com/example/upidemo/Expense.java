package com.example.upidemo;

import java.util.List;

public class Expense {
    public String expenseId;
    public String type; // "individual" or "group"
    public String groupId;
    public String paidBy;
    public double amount;
    public String merchant;
    public List<String> splitBetween;
    public long timestamp;

    public Expense() {}
    public Expense(String expenseId, String type, String groupId, String paidBy, double amount, String merchant, List<String> splitBetween, long timestamp) {
        this.expenseId = expenseId;
        this.type = type;
        this.groupId = groupId;
        this.paidBy = paidBy;
        this.amount = amount;
        this.merchant = merchant;
        this.splitBetween = splitBetween;
        this.timestamp = timestamp;
    }
}
