package com.example.walletwise;

public class Transaction {
    private String title;      // e.g., "Zepto Grocery"
    private String category;   // e.g., "Food & Drinks"
    private String date;       // e.g., "Today"
    private float amount;      // e.g., 450
    private boolean isCredit;  // true for income (+), false for expense (-)

    public Transaction(String title, String category, String date, float amount, boolean isCredit) {
        this.title = title;
        this.category = category;
        this.date = date;
        this.amount = amount;
        this.isCredit = isCredit;
    }

    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public float getAmount() { return amount; }
    public boolean isCredit() { return isCredit; }
}