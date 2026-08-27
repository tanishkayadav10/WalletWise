package com.example.walletwise;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BudgetDialogFragement extends BottomSheetDialogFragment {

    private EditText etTotalBudget;
    private EditText etHouseRent;
    private EditText etGroceries;
    private EditText etFuel;
    private EditText etPersonal;
    private EditText etSubscriptions;
    private EditText etCustomCategory;
    private EditText etCustomAmount;
    private Button btnSaveContinue;

    private String currentUserEmail = "";
    private String currentMonth = "";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.dialog_budget_setup,
                container,
                false
        );

        etTotalBudget = view.findViewById(R.id.etTotalBudget);
        etHouseRent = view.findViewById(R.id.etHouseRent);
        etGroceries = view.findViewById(R.id.etGroceries);
        etFuel = view.findViewById(R.id.etFuel);
        etPersonal = view.findViewById(R.id.etPersonal);
        etSubscriptions = view.findViewById(R.id.etSubscriptions);
        etCustomCategory = view.findViewById(R.id.etCustomCategory);
        etCustomAmount = view.findViewById(R.id.etCustomAmount);
        btnSaveContinue = view.findViewById(R.id.btnSaveContinue);

        setInputTextColors();
        loadCurrentUser();
        currentMonth = getCurrentMonth();
        loadExistingData();

        if (btnSaveContinue != null) {
            btnSaveContinue.setOnClickListener(
                    v -> saveBudgetData()
            );
        }

        return view;
    }

    private void setInputTextColors() {

        if (etTotalBudget != null) {
            etTotalBudget.setTextColor(Color.BLACK);
            etTotalBudget.setHintTextColor(Color.DKGRAY);
        }

        if (etHouseRent != null) {
            etHouseRent.setTextColor(Color.BLACK);
            etHouseRent.setHintTextColor(Color.DKGRAY);
        }

        if (etGroceries != null) {
            etGroceries.setTextColor(Color.BLACK);
            etGroceries.setHintTextColor(Color.DKGRAY);
        }

        if (etFuel != null) {
            etFuel.setTextColor(Color.BLACK);
            etFuel.setHintTextColor(Color.DKGRAY);
        }

        if (etPersonal != null) {
            etPersonal.setTextColor(Color.BLACK);
            etPersonal.setHintTextColor(Color.DKGRAY);
        }

        if (etSubscriptions != null) {
            etSubscriptions.setTextColor(Color.BLACK);
            etSubscriptions.setHintTextColor(Color.DKGRAY);
        }

        if (etCustomCategory != null) {
            etCustomCategory.setTextColor(Color.BLACK);
            etCustomCategory.setHintTextColor(Color.DKGRAY);
        }

        if (etCustomAmount != null) {
            etCustomAmount.setTextColor(Color.BLACK);
            etCustomAmount.setHintTextColor(Color.DKGRAY);
        }
    }

    private void loadCurrentUser() {

        Context context = getContext();

        if (context == null) {
            return;
        }

        SharedPreferences globalPrefs =
                context.getSharedPreferences(
                        "UserGlobalPrefs",
                        Context.MODE_PRIVATE
                );

        currentUserEmail =
                globalPrefs.getString(
                        "logged_in_email",
                        ""
                );

        if (currentUserEmail == null) {
            currentUserEmail = "";
        }

        currentUserEmail = currentUserEmail.trim();
    }

    private String getUserDataKey() {

        if (currentUserEmail.isEmpty()) {
            return "";
        }

        return "Data_" + currentUserEmail.replace(".", "_");
    }

    private String getCurrentMonth() {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "yyyy-MM",
                        Locale.getDefault()
                );

        return formatter.format(new Date());
    }

    private void loadExistingData() {

        if (currentUserEmail.isEmpty()) {
            return;
        }

        Context context = getContext();

        if (context == null) {
            return;
        }

        String dataKey = getUserDataKey();

        if (dataKey.isEmpty()) {
            return;
        }

        SharedPreferences dataPrefs =
                context.getSharedPreferences(
                        dataKey,
                        Context.MODE_PRIVATE
                );

        float budget =
                dataPrefs.getFloat(
                        "user_budget",
                        0f
                );

        if (budget > 0) {
            etTotalBudget.setText(formatNumber(budget));
        }

        String json =
                dataPrefs.getString(
                        "transactions_list",
                        "[]"
                );

        try {

            JSONArray transactions = new JSONArray(json);

            for (int i = 0; i < transactions.length(); i++) {

                JSONObject transaction =
                        transactions.getJSONObject(i);

                String title =
                        transaction.optString("title", "");

                String category =
                        transaction.optString("category", "");

                float amount =
                        (float) transaction.optDouble(
                                "amount",
                                0
                        );

                String transactionMonth =
                        transaction.optString(
                                "month",
                                ""
                        );

                if (!currentMonth.equals(transactionMonth)) {
                    continue;
                }

                if (title.equals("House Rent")
                        || category.equals("Rent")) {

                    etHouseRent.setText(formatNumber(amount));

                } else if (title.equals("Groceries")
                        || category.equals("Groceries")) {

                    etGroceries.setText(formatNumber(amount));

                } else if (title.equals("Fuel / Transport")
                        || category.equals("Fuel")) {

                    etFuel.setText(formatNumber(amount));

                } else if (title.equals("Personal Expenses")
                        || category.equals("Personal")) {

                    etPersonal.setText(formatNumber(amount));

                } else if (title.equals("Subscriptions")
                        || category.equals("Subscriptions")) {

                    etSubscriptions.setText(formatNumber(amount));

                } else {

                    if (etCustomCategory.getText()
                            .toString()
                            .trim()
                            .isEmpty()) {

                        etCustomCategory.setText(title);
                        etCustomAmount.setText(
                                formatNumber(amount)
                        );
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        setInputTextColors();
    }

    private String formatNumber(float number) {

        if (number == (long) number) {
            return String.valueOf((long) number);
        }

        return String.format(
                Locale.getDefault(),
                "%.2f",
                number
        );
    }

    private float parseInput(EditText editText) {

        if (editText == null) {
            return 0f;
        }

        String value =
                editText.getText()
                        .toString()
                        .trim();

        if (TextUtils.isEmpty(value)) {
            return 0f;
        }

        try {
            return Float.parseFloat(value);

        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private void saveBudgetData() {

        if (currentUserEmail.isEmpty()) {

            Toast.makeText(
                    getContext(),
                    "Please login first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String budgetText =
                etTotalBudget.getText()
                        .toString()
                        .trim();

        if (TextUtils.isEmpty(budgetText)) {

            etTotalBudget.setError(
                    "Enter your monthly budget"
            );

            etTotalBudget.requestFocus();

            return;
        }

        float totalBudget = parseInput(etTotalBudget);

        if (totalBudget <= 0) {

            etTotalBudget.setError(
                    "Enter a valid budget"
            );

            etTotalBudget.requestFocus();

            return;
        }

        float houseRent = parseInput(etHouseRent);
        float groceries = parseInput(etGroceries);
        float fuel = parseInput(etFuel);
        float personal = parseInput(etPersonal);
        float subscriptions = parseInput(etSubscriptions);

        String customCategory =
                etCustomCategory.getText()
                        .toString()
                        .trim();

        float customAmount =
                parseInput(etCustomAmount);

        if (!customCategory.isEmpty()
                && customAmount <= 0) {

            etCustomAmount.setError("Enter amount");
            etCustomAmount.requestFocus();
            return;
        }

        if (customCategory.isEmpty()
                && customAmount > 0) {

            etCustomCategory.setError(
                    "Enter category name"
            );

            etCustomCategory.requestFocus();

            return;
        }

        Context context = getContext();

        if (context == null) {
            return;
        }

        String dataKey = getUserDataKey();

        if (dataKey.isEmpty()) {
            return;
        }

        SharedPreferences dataPrefs =
                context.getSharedPreferences(
                        dataKey,
                        Context.MODE_PRIVATE
                );

        JSONArray oldTransactions =
                getExistingTransactions(dataPrefs);

        JSONArray newTransactions = new JSONArray();

        try {

            for (int i = 0;
                 i < oldTransactions.length();
                 i++) {

                JSONObject oldTransaction =
                        oldTransactions.getJSONObject(i);

                String month =
                        oldTransaction.optString(
                                "month",
                                ""
                        );

                String source =
                        oldTransaction.optString(
                                "source",
                                ""
                        );

                if (!currentMonth.equals(month)
                        || !source.equals("budget_form")) {

                    newTransactions.put(oldTransaction);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        addTransaction(
                newTransactions,
                "House Rent",
                houseRent,
                "Rent"
        );

        addTransaction(
                newTransactions,
                "Groceries",
                groceries,
                "Groceries"
        );

        addTransaction(
                newTransactions,
                "Fuel / Transport",
                fuel,
                "Fuel"
        );

        addTransaction(
                newTransactions,
                "Personal Expenses",
                personal,
                "Personal"
        );

        addTransaction(
                newTransactions,
                "Subscriptions",
                subscriptions,
                "Subscriptions"
        );

        if (!customCategory.isEmpty()
                && customAmount > 0) {

            addTransaction(
                    newTransactions,
                    customCategory,
                    customAmount,
                    customCategory
            );
        }

        float totalExpenses =
                calculateTotalExpenses(newTransactions);

        float totalBalance =
                totalBudget - totalExpenses;

        dataPrefs.edit()
                .putFloat(
                        "user_budget",
                        totalBudget
                )
                .putString(
                        "budget_month",
                        currentMonth
                )
                .putString(
                        "transactions_list",
                        newTransactions.toString()
                )
                .putFloat(
                        "total_expenses",
                        totalExpenses
                )
                .putFloat(
                        "total_balance",
                        totalBalance
                )
                .putBoolean(
                        "setup_completed",
                        true
                )
                .apply();

        saveToFirebase(
                totalBudget,
                houseRent,
                groceries,
                fuel,
                personal,
                subscriptions,
                customCategory,
                customAmount,
                totalExpenses,
                totalBalance
        );
    }

    private void addTransaction(
            JSONArray transactions,
            String title,
            float amount,
            String category) {

        if (amount <= 0) {
            return;
        }

        try {

            JSONObject transaction = new JSONObject();

            transaction.put("title", title);
            transaction.put("amount", (double) amount);
            transaction.put("category", category);
            transaction.put("month", currentMonth);
            transaction.put(
                    "date",
                    System.currentTimeMillis()
            );
            transaction.put(
                    "source",
                    "budget_form"
            );

            transactions.put(transaction);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONArray getExistingTransactions(
            SharedPreferences dataPrefs) {

        String existing =
                dataPrefs.getString(
                        "transactions_list",
                        "[]"
                );

        try {

            if (TextUtils.isEmpty(existing)) {
                return new JSONArray();
            }

            return new JSONArray(existing);

        } catch (Exception e) {

            e.printStackTrace();

            return new JSONArray();
        }
    }

    private float calculateTotalExpenses(
            JSONArray transactions) {

        float total = 0f;

        try {

            for (int i = 0;
                 i < transactions.length();
                 i++) {

                JSONObject transaction =
                        transactions.getJSONObject(i);

                total +=
                        (float) transaction.optDouble(
                                "amount",
                                0
                        );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return total;
    }

    private void saveToFirebase(
            float totalBudget,
            float houseRent,
            float groceries,
            float fuel,
            float personal,
            float subscriptions,
            String customCategory,
            float customAmount,
            float totalExpenses,
            float totalBalance) {

        new Thread(() -> {

            try {

                FirebaseDatabase database =
                        FirebaseDatabase.getInstance(
                                "https://myapplication-da3a3270-default-rtdb.firebaseio.com/"
                        );

                DatabaseReference usersRef =
                        database.getReference("users");

                String userEmailKey =
                        currentUserEmail.replace(
                                ".",
                                "_"
                        );

                Map<String, Object> budgetData =
                        new HashMap<>();

                budgetData.put(
                        "total_budget",
                        totalBudget
                );

                budgetData.put(
                        "total_expenses",
                        totalExpenses
                );

                budgetData.put(
                        "total_balance",
                        totalBalance
                );

                budgetData.put(
                        "cat_rent",
                        houseRent
                );

                budgetData.put(
                        "cat_groceries",
                        groceries
                );

                budgetData.put(
                        "cat_fuel",
                        fuel
                );

                budgetData.put(
                        "cat_personal",
                        personal
                );

                budgetData.put(
                        "cat_subscriptions",
                        subscriptions
                );

                budgetData.put(
                        "budget_month",
                        currentMonth
                );

                if (!customCategory.isEmpty()
                        && customAmount > 0) {

                    budgetData.put(
                            "custom_category",
                            customCategory
                    );

                    budgetData.put(
                            "custom_amount",
                            customAmount
                    );

                } else {

                    budgetData.put(
                            "custom_category",
                            null
                    );

                    budgetData.put(
                            "custom_amount",
                            null
                    );
                }

                usersRef
                        .child(userEmailKey)
                        .updateChildren(budgetData)
                        .addOnCompleteListener(
                                task -> {

                                    if (task.isSuccessful()) {

                                        showSavedMessage(
                                                "Budget & expenses saved successfully! ✓"
                                        );

                                    } else {

                                        showSavedMessage(
                                                "Saved locally!"
                                        );
                                    }
                                }
                        );

            } catch (Exception e) {

                e.printStackTrace();

                showSavedMessage(
                        "Saved locally!"
                );
            }

        }).start();
    }

    private void showSavedMessage(
            String message) {

        if (!isAdded()) {
            return;
        }

        requireActivity().runOnUiThread(() -> {

            Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
            ).show();

            dismiss();

            if (getActivity() != null) {
                getActivity().recreate();
            }
        });
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        etTotalBudget = null;
        etHouseRent = null;
        etGroceries = null;
        etFuel = null;
        etPersonal = null;
        etSubscriptions = null;
        etCustomCategory = null;
        etCustomAmount = null;
        btnSaveContinue = null;
    }
}