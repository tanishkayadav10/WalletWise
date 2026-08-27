package com.example.walletwise;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private EditText etEmail;

    private Spinner spinnerRole;

    private Button btnSignUp;
    private Button btnLogin;

    private ImageButton btnToggleSignupPassword;

    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signup);

        etUsername = findViewById(R.id.editTextText);
        etPassword = findViewById(R.id.editTextTextPassword);
        etEmail = findViewById(R.id.editTextText2);

        spinnerRole = findViewById(R.id.spinnerRole);

        btnSignUp = findViewById(R.id.button2);
        btnLogin = findViewById(R.id.button4);

        btnToggleSignupPassword =
                findViewById(R.id.btnToggleSignupPassword);

        if (btnToggleSignupPassword != null) {

            btnToggleSignupPassword.setOnClickListener(v -> {

                if (passwordVisible) {

                    etPassword.setInputType(
                            InputType.TYPE_CLASS_TEXT |
                                    InputType.TYPE_TEXT_VARIATION_PASSWORD
                    );

                    passwordVisible = false;

                } else {

                    etPassword.setInputType(
                            InputType.TYPE_CLASS_TEXT |
                                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    );

                    passwordVisible = true;
                }

                etPassword.setSelection(
                        etPassword.getText().length()
                );
            });
        }

        String[] roles = {
                "Student",
                "Working Professional"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        roles
                );

        spinnerRole.setAdapter(adapter);

        btnSignUp.setOnClickListener(
                v -> performSignUp()
        );

        btnLogin.setOnClickListener(v -> {

            Intent intent = new Intent(
                    SignUpActivity.this,
                    LoginActivity.class
            );

            startActivity(intent);
            finish();
        });
    }

    private void performSignUp() {

        String username =
                etUsername.getText()
                        .toString()
                        .trim()
                        .toLowerCase();

        String password =
                etPassword.getText()
                        .toString()
                        .trim();

        String email =
                etEmail.getText()
                        .toString()
                        .trim()
                        .toLowerCase();

        String selectedRole =
                spinnerRole.getSelectedItem()
                        .toString();

        if (TextUtils.isEmpty(username)) {

            Toast.makeText(
                    this,
                    "Please enter username",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (TextUtils.isEmpty(password)) {

            Toast.makeText(
                    this,
                    "Please enter password",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (TextUtils.isEmpty(email)) {

            Toast.makeText(
                    this,
                    "Please enter email",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!email.contains("@")) {

            Toast.makeText(
                    this,
                    "Please enter a valid email",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        SharedPreferences accountIndex =
                getSharedPreferences(
                        "UserAccountIndex",
                        Context.MODE_PRIVATE
                );

        String existingEmail =
                accountIndex.getString(
                        username,
                        ""
                );

        if (!existingEmail.isEmpty()) {

            Toast.makeText(
                    this,
                    "Username already exists.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        SharedPreferences existingUser =
                getSharedPreferences(
                        "UserPrefs_" + email,
                        Context.MODE_PRIVATE
                );

        String oldEmail =
                existingUser.getString(
                        "user_email",
                        ""
                );

        if (!oldEmail.isEmpty()) {

            Toast.makeText(
                    this,
                    "An account with this email already exists.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        accountIndex.edit()
                .putString(username, email)
                .apply();

        SharedPreferences userPrefs =
                getSharedPreferences(
                        "UserPrefs_" + email,
                        Context.MODE_PRIVATE
                );

        userPrefs.edit()
                .putString("user_name", username)
                .putString("user_email", email)
                .putString("user_password", password)
                .putString("user_role", selectedRole)
                .apply();

        String dataKey =
                "Data_" + email.replace(".", "_");

        SharedPreferences dataPrefs =
                getSharedPreferences(
                        dataKey,
                        Context.MODE_PRIVATE
                );

        dataPrefs.edit()
                .putBoolean("setup_completed", false)
                .putFloat("user_budget", 0f)
                .putFloat("total_expenses", 0f)
                .putFloat("total_balance", 0f)
                .putString("transactions_list", "[]")
                .apply();

        SharedPreferences globalPrefs =
                getSharedPreferences(
                        "UserGlobalPrefs",
                        Context.MODE_PRIVATE
                );

        globalPrefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("logged_in_email", email)
                .apply();

        try {

            String firebaseKey =
                    email.replace(".", ",");

            DatabaseReference userRef =
                    FirebaseDatabase
                            .getInstance(
                                    "https://myapplication-da3a3270-default-rtdb.firebaseio.com/"
                            )
                            .getReference("users")
                            .child(firebaseKey);

            Map<String, Object> initialData =
                    new HashMap<>();

            initialData.put(
                    "user_name",
                    username
            );

            initialData.put(
                    "user_email",
                    email
            );

            initialData.put(
                    "user_role",
                    selectedRole
            );

            initialData.put(
                    "total_budget",
                    0f
            );

            initialData.put(
                    "total_expenses",
                    0f
            );

            initialData.put(
                    "total_balance",
                    0f
            );

            userRef.setValue(initialData);

        } catch (Exception e) {
            e.printStackTrace();
        }

        Toast.makeText(
                this,
                "Account Created Successfully!",
                Toast.LENGTH_SHORT
        ).show();

        Intent intent =
                new Intent(
                        SignUpActivity.this,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}