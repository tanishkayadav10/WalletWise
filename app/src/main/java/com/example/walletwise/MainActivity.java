package com.example.walletwise;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // =========================================================
    // TEXT VIEWS
    // =========================================================

    private TextView tvWelcome;
    private TextView tvTotalBalance;
    private TextView tvBudget;
    private TextView tvExpenses;
    private TextView tvViewAll;

    // =========================================================
    // VIEWS
    // =========================================================

    private CardView cardBudgetSetup;
    private Button btnEditExpenses;

    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAiChat;

    private LinearLayout recentActivityContainer;

    // =========================================================
    // CHARTS
    // =========================================================

    private LineChart lineChart;
    private PieChart pieChart;

    // =========================================================
    // USER
    // =========================================================

    private String currentEmail = "";

    private boolean firstSetupChecked = false;

    // =========================================================
    // MONTH NAMES
    // =========================================================

    private final String[] MONTH_NAMES = {
            "Jan", "Feb", "Mar", "Apr",
            "May", "Jun", "Jul", "Aug",
            "Sep", "Oct", "Nov", "Dec"
    };

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // =====================================================
        // LOAD CURRENT USER
        // =====================================================

        loadCurrentUser();

        // =====================================================
        // FIND VIEWS
        // =====================================================

        tvWelcome = findViewById(R.id.tvWelcome);

        tvTotalBalance = findViewById(R.id.tvTotalBalance);

        tvBudget = findViewById(R.id.tvBudget);

        tvExpenses = findViewById(R.id.tvExpenses);

        tvViewAll = findViewById(R.id.tvViewAll);

        cardBudgetSetup = findViewById(R.id.cardBudgetSetup);

        btnEditExpenses = findViewById(R.id.btnEditExpenses);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        fabAiChat = findViewById(R.id.fabAiChat);

        recentActivityContainer =
                findViewById(R.id.recentActivityContainer);

        lineChart = findViewById(R.id.lineChart);

        pieChart = findViewById(R.id.pieChart);

        // =====================================================
        // LOAD DASHBOARD
        // =====================================================

        loadWelcomeName();

        loadDashboardData();

        // =====================================================
        // FIRST TIME SETUP
        // =====================================================

        checkFirstTimeSetup();

        // =====================================================
        // BUDGET CARD
        // =====================================================

        if (cardBudgetSetup != null) {

            cardBudgetSetup.setOnClickListener(v ->
                    openBudgetDialog()
            );
        }

        // =====================================================
        // EDIT EXPENSES
        // =====================================================

        if (btnEditExpenses != null) {

            btnEditExpenses.setOnClickListener(v ->
                    openBudgetDialog()
            );
        }

        // =====================================================
        // AI CHAT
        // =====================================================

        if (fabAiChat != null) {

            fabAiChat.setOnClickListener(v ->
                    openAIChat()
            );
        }

        // =====================================================
        // VIEW ALL TRANSACTIONS
        // =====================================================

        if (tvViewAll != null) {

            tvViewAll.setOnClickListener(v ->
                    showAllTransactions()
            );
        }

        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        setupBottomNavigation();
    }

    // =========================================================
    // FIRST TIME SETUP
    // =========================================================

    private void checkFirstTimeSetup() {

        if (firstSetupChecked) {
            return;
        }

        firstSetupChecked = true;

        if (currentEmail == null ||
                currentEmail.trim().isEmpty()) {

            return;
        }

        String dataKey = getUserDataKey();

        if (dataKey.isEmpty()) {
            return;
        }

        SharedPreferences dataPrefs =
                getSharedPreferences(
                        dataKey,
                        Context.MODE_PRIVATE
                );

        boolean setupCompleted =
                dataPrefs.getBoolean(
                        "setup_completed",
                        false
                );

        if (!setupCompleted) {

            new android.os.Handler(
                    android.os.Looper.getMainLooper()
            ).postDelayed(() -> {

                if (!isFinishing() &&
                        !isDestroyed()) {

                    openBudgetDialog();
                }

            }, 400);
        }
    }

    // =========================================================
    // OPEN BUDGET DIALOG
    // =========================================================

    private void openBudgetDialog() {

        if (currentEmail == null ||
                currentEmail.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Please login first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (getSupportFragmentManager()
                .findFragmentByTag("BudgetDialog") != null) {

            return;
        }

        BudgetDialogFragement dialog =
                new BudgetDialogFragement();

        dialog.show(
                getSupportFragmentManager(),
                "BudgetDialog"
        );
    }

    // =========================================================
    // LOAD CURRENT USER
    // =========================================================

    private void loadCurrentUser() {

        SharedPreferences prefs =
                getSharedPreferences(
                        "UserGlobalPrefs",
                        Context.MODE_PRIVATE
                );

        currentEmail =
                prefs.getString(
                        "logged_in_email",
                        ""
                );

        if (currentEmail == null) {
            currentEmail = "";
        }

        currentEmail = currentEmail.trim();
    }

    // =========================================================
    // USER DATA KEY
    // =========================================================

    private String getUserDataKey() {

        if (currentEmail == null ||
                currentEmail.trim().isEmpty()) {

            return "";
        }

        return "Data_" +
                currentEmail
                        .trim()
                        .replace(".", "_");
    }

    // =========================================================
    // LOAD WELCOME NAME
    // =========================================================

    private void loadWelcomeName() {

        if (tvWelcome == null) {
            return;
        }

        if (currentEmail.isEmpty()) {

            tvWelcome.setText("Welcome!");

            return;
        }

        SharedPreferences userPrefs =
                getSharedPreferences(
                        "UserPrefs_" + currentEmail,
                        Context.MODE_PRIVATE
                );

        String name =
                userPrefs.getString(
                        "user_name",
                        "User"
                );

        tvWelcome.setText(
                "Welcome, " + name + "!"
        );
    }

    // =========================================================
    // LOAD DASHBOARD
    // =========================================================

    private void loadDashboardData() {

        if (currentEmail.isEmpty()) {

            showEmptyDashboard();

            return;
        }

        String dataKey = getUserDataKey();

        if (dataKey.isEmpty()) {

            showEmptyDashboard();

            return;
        }

        SharedPreferences dataPrefs =
                getSharedPreferences(
                        dataKey,
                        Context.MODE_PRIVATE
                );

        float budget =
                dataPrefs.getFloat(
                        "user_budget",
                        0f
                );

        String json =
                dataPrefs.getString(
                        "transactions_list",
                        "[]"
                );

        float totalExpenses =
                calculateExpenses(json);

        float balance =
                budget - totalExpenses;

        // =====================================================
        // BUDGET
        // =====================================================

        if (tvBudget != null) {

            tvBudget.setText(
                    "₹" +
                            String.format(
                                    Locale.getDefault(),
                                    "%.2f",
                                    budget
                            )
            );
        }

        // =====================================================
        // EXPENSES
        // =====================================================

        if (tvExpenses != null) {

            tvExpenses.setText(
                    "₹" +
                            String.format(
                                    Locale.getDefault(),
                                    "%.2f",
                                    totalExpenses
                            )
            );
        }

        // =====================================================
        // BALANCE
        // =====================================================

        if (tvTotalBalance != null) {

            tvTotalBalance.setText(
                    "₹" +
                            String.format(
                                    Locale.getDefault(),
                                    "%.2f",
                                    balance
                            )
            );
        }

        // =====================================================
        // SAVE CALCULATED VALUES
        // =====================================================

        dataPrefs.edit()
                .putFloat(
                        "total_expenses",
                        totalExpenses
                )
                .putFloat(
                        "total_balance",
                        balance
                )
                .apply();

        // =====================================================
        // LOAD EVERYTHING
        // =====================================================

        loadPieChart();

        loadLineChart();

        loadRecentTransactions();
    }

    // =========================================================
    // CALCULATE EXPENSES
    // =========================================================

    private float calculateExpenses(String json) {

        float total = 0f;

        if (json == null ||
                json.trim().isEmpty()) {

            return 0f;
        }

        try {

            JSONArray array =
                    new JSONArray(json);

            for (int i = 0;
                 i < array.length();
                 i++) {

                JSONObject obj =
                        array.getJSONObject(i);

                float amount =
                        (float) obj.optDouble(
                                "amount",
                                0
                        );

                if (amount > 0) {

                    total += amount;
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return total;
    }

    // =========================================================
    // EMPTY DASHBOARD
    // =========================================================

    private void showEmptyDashboard() {

        if (tvBudget != null) {
            tvBudget.setText("₹0.00");
        }

        if (tvExpenses != null) {
            tvExpenses.setText("₹0.00");
        }

        if (tvTotalBalance != null) {
            tvTotalBalance.setText("₹0.00");
        }

        if (recentActivityContainer != null) {

            recentActivityContainer.removeAllViews();
        }

        if (pieChart != null) {

            pieChart.clear();

            pieChart.setCenterText(
                    "No Expenses"
            );

            pieChart.invalidate();
        }

        if (lineChart != null) {

            lineChart.clear();

            lineChart.invalidate();
        }
    }

    // =========================================================
    // PIE CHART
    // =========================================================

    private void loadPieChart() {

        if (pieChart == null ||
                currentEmail.isEmpty()) {

            return;
        }

        pieChart.clear();

        String dataKey = getUserDataKey();

        if (dataKey.isEmpty()) {
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences(
                        dataKey,
                        Context.MODE_PRIVATE
                );

        String json =
                prefs.getString(
                        "transactions_list",
                        "[]"
                );

        HashMap<String, Float> totals =
                new HashMap<>();

        try {

            JSONArray array =
                    new JSONArray(json);

            for (int i = 0;
                 i < array.length();
                 i++) {

                JSONObject obj =
                        array.getJSONObject(i);

                String category =
                        obj.optString(
                                "category",
                                "Other"
                        );

                float amount =
                        (float) obj.optDouble(
                                "amount",
                                0
                        );

                if (amount <= 0) {
                    continue;
                }

                Float old =
                        totals.get(category);

                if (old == null) {
                    old = 0f;
                }

                totals.put(
                        category,
                        old + amount
                );
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        ArrayList<PieEntry> entries =
                new ArrayList<>();

        for (Map.Entry<String, Float> item :
                totals.entrySet()) {

            if (item.getValue() > 0) {

                entries.add(
                        new PieEntry(
                                item.getValue(),
                                item.getKey()
                        )
                );
            }
        }

        if (entries.isEmpty()) {

            pieChart.setCenterText(
                    "No Expenses"
            );

            pieChart.setCenterTextSize(18f);

            pieChart.invalidate();

            return;
        }

        PieDataSet dataSet =
                new PieDataSet(
                        entries,
                        "Categories"
                );

        ArrayList<Integer> colors =
                new ArrayList<>();

        colors.add(Color.rgb(156, 39, 176));
        colors.add(Color.rgb(33, 150, 243));
        colors.add(Color.rgb(0, 200, 83));
        colors.add(Color.rgb(255, 193, 7));
        colors.add(Color.rgb(255, 87, 34));
        colors.add(Color.rgb(233, 30, 99));
        colors.add(Color.rgb(0, 188, 212));
        colors.add(Color.rgb(121, 85, 72));
        colors.add(Color.rgb(63, 81, 181));
        colors.add(Color.rgb(76, 175, 80));
        colors.add(Color.rgb(255, 112, 67));
        colors.add(Color.rgb(103, 58, 183));

        dataSet.setColors(colors);

        dataSet.setValueTextSize(12f);

        dataSet.setValueTextColor(
                Color.WHITE
        );

        PieData data =
                new PieData(dataSet);

        pieChart.setData(data);

        pieChart.getDescription()
                .setEnabled(false);

        pieChart.setCenterText(
                "Spending"
        );

        pieChart.setCenterTextSize(18f);

        pieChart.setCenterTextColor(
                Color.WHITE
        );

        pieChart.setHoleRadius(55f);

        pieChart.setTransparentCircleRadius(
                60f
        );

        pieChart.setEntryLabelColor(
                Color.WHITE
        );

        pieChart.setEntryLabelTextSize(
                11f
        );

        pieChart.getLegend()
                .setEnabled(true);

        pieChart.getLegend()
                .setTextColor(
                        Color.WHITE
                );

        pieChart.animateY(800);

        pieChart.invalidate();
    }

    // =========================================================
    // LINE CHART
    // =========================================================

    private void loadLineChart() {

        if (lineChart == null ||
                currentEmail.isEmpty()) {

            return;
        }

        lineChart.clear();

        String dataKey = getUserDataKey();

        if (dataKey.isEmpty()) {
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences(
                        dataKey,
                        Context.MODE_PRIVATE
                );

        String json =
                prefs.getString(
                        "transactions_list",
                        "[]"
                );

        float[] monthlyExpenses =
                new float[12];

        try {

            JSONArray array =
                    new JSONArray(json);

            for (int i = 0;
                 i < array.length();
                 i++) {

                JSONObject obj =
                        array.getJSONObject(i);

                float amount =
                        (float) obj.optDouble(
                                "amount",
                                0
                        );

                if (amount <= 0) {
                    continue;
                }

                long timestamp =
                        obj.optLong(
                                "date",
                                0
                        );

                if (timestamp <= 0) {

                    timestamp =
                            obj.optLong(
                                    "timestamp",
                                    0
                            );
                }

                if (timestamp <= 0) {
                    continue;
                }

                Date date =
                        new Date(timestamp);

                int month =
                        Integer.parseInt(
                                new SimpleDateFormat(
                                        "M",
                                        Locale.getDefault()
                                ).format(date)
                        ) - 1;

                if (month >= 0 &&
                        month < 12) {

                    monthlyExpenses[month] += amount;
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        ArrayList<Entry> entries =
                new ArrayList<>();

        for (int i = 0; i < 12; i++) {

            entries.add(
                    new Entry(
                            i,
                            monthlyExpenses[i]
                    )
            );
        }

        LineDataSet dataSet =
                new LineDataSet(
                        entries,
                        "Monthly Spending"
                );

        dataSet.setLineWidth(3f);

        dataSet.setCircleRadius(5f);

        dataSet.setCircleHoleRadius(2.5f);

        dataSet.setValueTextSize(10f);

        dataSet.setDrawValues(true);

        dataSet.setColor(
                Color.rgb(156, 39, 176)
        );

        dataSet.setCircleColor(
                Color.rgb(156, 39, 176)
        );

        dataSet.setValueTextColor(
                Color.WHITE
        );

        dataSet.setMode(
                LineDataSet.Mode.CUBIC_BEZIER
        );

        LineData lineData =
                new LineData(dataSet);

        lineChart.setData(lineData);

        lineChart.getDescription()
                .setEnabled(false);

        lineChart.getLegend()
                .setTextColor(
                        Color.WHITE
                );

        XAxis xAxis =
                lineChart.getXAxis();

        xAxis.setPosition(
                XAxis.XAxisPosition.BOTTOM
        );

        xAxis.setGranularity(1f);

        xAxis.setGranularityEnabled(true);

        xAxis.setLabelCount(
                12,
                true
        );

        xAxis.setTextColor(
                Color.WHITE
        );

        xAxis.setDrawGridLines(false);

        xAxis.setValueFormatter(
                new ValueFormatter() {

                    @Override
                    public String getAxisLabel(
                            float value,
                            com.github.mikephil.charting.components.AxisBase axis) {

                        int index =
                                Math.round(value);

                        if (index >= 0 &&
                                index < 12) {

                            return MONTH_NAMES[index];
                        }

                        return "";
                    }
                }
        );

        YAxis leftAxis =
                lineChart.getAxisLeft();

        leftAxis.setTextColor(
                Color.WHITE
        );

        leftAxis.setDrawGridLines(true);

        leftAxis.setAxisMinimum(0f);

        float monthlyBudget =
                prefs.getFloat(
                        "user_budget",
                        0f
                );

        float maxExpense = 0f;

        for (float value :
                monthlyExpenses) {

            if (value > maxExpense) {

                maxExpense = value;
            }
        }

        float yMax =
                Math.max(
                        monthlyBudget,
                        maxExpense
                );

        if (yMax <= 0) {

            yMax = 1000f;
        }

        yMax = yMax * 1.20f;

        leftAxis.setAxisMaximum(
                yMax
        );

        leftAxis.setValueFormatter(
                new ValueFormatter() {

                    @Override
                    public String getAxisLabel(
                            float value,
                            com.github.mikephil.charting.components.AxisBase axis) {

                        if (value >= 100000) {

                            return "₹" +
                                    String.format(
                                            Locale.getDefault(),
                                            "%.1fL",
                                            value / 100000f
                                    );
                        }

                        if (value >= 1000) {

                            return "₹" +
                                    String.format(
                                            Locale.getDefault(),
                                            "%.1fK",
                                            value / 1000f
                                    );
                        }

                        return "₹" +
                                String.format(
                                        Locale.getDefault(),
                                        "%.0f",
                                        value
                                );
                    }
                }
        );

        lineChart.getAxisRight()
                .setEnabled(false);

        lineChart.setTouchEnabled(true);

        lineChart.setDragEnabled(true);

        lineChart.setScaleEnabled(false);

        lineChart.setPinchZoom(false);

        lineChart.animateX(900);

        lineChart.invalidate();
    }

    // =========================================================
    // RECENT TRANSACTIONS
    // =========================================================

    private void loadRecentTransactions() {

        if (recentActivityContainer == null ||
                currentEmail.isEmpty()) {

            return;
        }

        recentActivityContainer.removeAllViews();

        String dataKey = getUserDataKey();

        if (dataKey.isEmpty()) {
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences(
                        dataKey,
                        Context.MODE_PRIVATE
                );

        String json =
                prefs.getString(
                        "transactions_list",
                        "[]"
                );

        try {

            JSONArray array =
                    new JSONArray(json);

            if (array.length() == 0) {

                TextView empty =
                        new TextView(this);

                empty.setText(
                        "No transactions yet."
                );

                empty.setTextColor(
                        Color.LTGRAY
                );

                empty.setTextSize(14f);

                empty.setPadding(
                        8,
                        16,
                        8,
                        16
                );

                recentActivityContainer
                        .addView(empty);

                return;
            }

            int start =
                    Math.max(
                            0,
                            array.length() - 5
                    );

            for (int i = array.length() - 1;
                 i >= start;
                 i--) {

                addTransactionView(
                        array.getJSONObject(i),
                        recentActivityContainer
                );
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // GET CATEGORY ICON
    // =========================================================

    private int getCategoryIcon(String category) {

        if (category == null) {
            return R.drawable.ic_custom;
        }

        String cleanCategory =
                category.trim().toLowerCase(Locale.getDefault());

        // =====================================================
        // HOUSE RENT
        // =====================================================

        if (cleanCategory.equals("house rent") ||
                cleanCategory.equals("rent") ||
                cleanCategory.equals("home") ||
                cleanCategory.contains("rent")) {

            return R.drawable.ic_house_rent;
        }

        // =====================================================
        // GROCERIES
        // =====================================================

        if (cleanCategory.equals("groceries") ||
                cleanCategory.equals("grocery") ||
                cleanCategory.contains("grocery") ||
                cleanCategory.contains("grocer")) {

            return R.drawable.ic_groceries;
        }

        // =====================================================
        // FUEL
        // =====================================================

        if (cleanCategory.equals("fuel") ||
                cleanCategory.contains("fuel") ||
                cleanCategory.contains("petrol") ||
                cleanCategory.contains("diesel")) {

            return R.drawable.ic_fuel;
        }

        // =====================================================
        // PERSONAL
        // =====================================================

        if (cleanCategory.equals("personal") ||
                cleanCategory.contains("personal")) {

            return R.drawable.ic_personal;
        }

        // =====================================================
        // SUBSCRIPTIONS
        // =====================================================

        if (cleanCategory.equals("subscriptions") ||
                cleanCategory.equals("subscription") ||
                cleanCategory.contains("subscription") ||
                cleanCategory.contains("netflix") ||
                cleanCategory.contains("spotify") ||
                cleanCategory.contains("prime")) {

            return R.drawable.ic_subscription;
        }

        // =====================================================
        // CUSTOM / ANYTHING ELSE
        // =====================================================

        return R.drawable.ic_custom;
    }

    // =========================================================
    // ADD TRANSACTION VIEW
    // =========================================================

    private void addTransactionView(
            JSONObject transaction,
            LinearLayout container) {

        try {

            View item =
                    LayoutInflater
                            .from(this)
                            .inflate(
                                    R.layout.item_transaction,
                                    container,
                                    false
                            );

            TextView title =
                    item.findViewById(
                            R.id.tvTxTitle
                    );

            TextView subtitle =
                    item.findViewById(
                            R.id.tvTxSubtitle
                    );

            TextView amount =
                    item.findViewById(
                            R.id.tvTxAmount
                    );

            // =================================================
            // FIND ICON VIEW
            // =================================================

            ImageView transactionIcon =
                    item.findViewById(
                            R.id.ivTxIcon
                    );

            // =================================================
            // READ TRANSACTION DATA
            // =================================================

            String txTitle =
                    transaction.optString(
                            "title",
                            "Expense"
                    );

            String category =
                    transaction.optString(
                            "category",
                            "Other"
                    );

            float txAmount =
                    (float) transaction.optDouble(
                            "amount",
                            0
                    );

            long timestamp =
                    transaction.optLong(
                            "date",
                            0
                    );

            String dateText =
                    "Recent";

            if (timestamp > 0) {

                dateText =
                        new SimpleDateFormat(
                                "dd MMM yyyy",
                                Locale.getDefault()
                        ).format(
                                new Date(timestamp)
                        );
            }

            // =================================================
            // SET TITLE
            // =================================================

            if (title != null) {

                title.setText(
                        txTitle
                );
            }

            // =================================================
            // SET SUBTITLE
            // =================================================

            if (subtitle != null) {

                subtitle.setText(
                        category +
                                " • " +
                                dateText
                );
            }

            // =================================================
            // SET AMOUNT
            // =================================================

            if (amount != null) {

                amount.setText(
                        "-₹" +
                                String.format(
                                        Locale.getDefault(),
                                        "%.2f",
                                        txAmount
                                )
                );

                amount.setTextColor(
                        Color.rgb(
                                255,
                                82,
                                82
                        )
                );
            }

            // =================================================
            // SET CATEGORY ICON
            // =================================================

            if (transactionIcon != null) {

                int iconRes =
                        getCategoryIcon(
                                category
                        );

                transactionIcon.setImageResource(
                        iconRes
                );
            }

            // =================================================
            // ADD ITEM
            // =================================================

            container.addView(item);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // VIEW ALL TRANSACTIONS
    // =========================================================

    private void showAllTransactions() {

        if (currentEmail.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please login first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String dataKey = getUserDataKey();

        if (dataKey.isEmpty()) {
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences(
                        dataKey,
                        Context.MODE_PRIVATE
                );

        String json =
                prefs.getString(
                        "transactions_list",
                        "[]"
                );

        try {

            JSONArray array =
                    new JSONArray(json);

            if (array.length() == 0) {

                new AlertDialog.Builder(this)
                        .setTitle(
                                "All Transactions"
                        )
                        .setMessage(
                                "No transactions yet."
                        )
                        .setPositiveButton(
                                "OK",
                                null
                        )
                        .show();

                return;
            }

            LinearLayout container =
                    new LinearLayout(this);

            container.setOrientation(
                    LinearLayout.VERTICAL
            );

            container.setPadding(
                    12,
                    12,
                    12,
                    12
            );

            container.setBackgroundColor(
                    Color.rgb(
                            20,
                            20,
                            23
                    )
            );

            ScrollView scrollView =
                    new ScrollView(this);

            scrollView.addView(
                    container
            );

            // =================================================
            // NEWEST FIRST
            // =================================================

            for (int i = array.length() - 1;
                 i >= 0;
                 i--) {

                addTransactionView(
                        array.getJSONObject(i),
                        container
                );
            }

            AlertDialog dialog =
                    new AlertDialog.Builder(this)
                            .setTitle(
                                    "All Transactions"
                            )
                            .setView(
                                    scrollView
                            )
                            .setPositiveButton(
                                    "Close",
                                    null
                            )
                            .create();

            dialog.show();

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Could not load transactions.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private void setupBottomNavigation() {

        if (bottomNavigation == null) {
            return;
        }

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id =
                    item.getItemId();

            // =================================================
            // HOME
            // =================================================

            if (id == R.id.nav_home) {

                return true;
            }

            // =================================================
            // AI CHAT
            // =================================================

            if (id == R.id.nav_ai_chat) {

                openAIChat();

                return true;
            }

            // =================================================
            // PROFILE
            // =================================================

            if (id == R.id.nav_profile) {

                Intent intent =
                        new Intent(
                                MainActivity.this,
                                ProfileActivity.class
                        );

                startActivity(intent);

                return true;
            }

            return false;
        });

        bottomNavigation.setSelectedItemId(
                R.id.nav_home
        );
    }

    // =========================================================
    // AI CHAT
    // =========================================================

    private void openAIChat() {

        try {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            AIChatActivity.class
                    );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "AI Chat could not be opened.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // REFRESH WHEN RETURNING
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        loadCurrentUser();

        loadWelcomeName();

        loadDashboardData();

        if (bottomNavigation != null) {

            bottomNavigation.setSelectedItemId(
                    R.id.nav_home
            );
        }
    }
}