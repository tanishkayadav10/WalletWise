package com.example.walletwise;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.walletwise.R;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;

    private Button btnLogin;
    private TextView tvSignUp;

    private ImageButton btnTogglePassword;

    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);

        btnTogglePassword = findViewById(R.id.btnTogglePassword);

        // -----------------------------------------------------
        // SHOW / HIDE PASSWORD
        // -----------------------------------------------------

        if (btnTogglePassword != null && etPassword != null) {

            btnTogglePassword.setOnClickListener(v -> {

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

        // -----------------------------------------------------
        // If already logged in, go directly to MainActivity
        // -----------------------------------------------------

        SharedPreferences globalPrefs =
                getSharedPreferences(
                        "UserGlobalPrefs",
                        Context.MODE_PRIVATE
                );

        boolean isLoggedIn =
                globalPrefs.getBoolean(
                        "is_logged_in",
                        false
                );

        String loggedInEmail =
                globalPrefs.getString(
                        "logged_in_email",
                        ""
                );

        if (isLoggedIn &&
                loggedInEmail != null &&
                !loggedInEmail.trim().isEmpty()) {

            openMainActivity(loggedInEmail);

            return;
        }

        // -----------------------------------------------------
        // LOGIN BUTTON
        // -----------------------------------------------------

        if (btnLogin != null) {

            btnLogin.setOnClickListener(v ->
                    performLogin()
            );
        }

        // -----------------------------------------------------
        // SIGN UP
        // -----------------------------------------------------

        if (tvSignUp != null) {

            tvSignUp.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                LoginActivity.this,
                                SignUpActivity.class
                        );

                startActivity(intent);
            });
        }
    }

    // =========================================================
    // LOGIN
    // =========================================================

    private void performLogin() {

        if (etEmail == null ||
                etPassword == null) {

            return;
        }

        String loginValue =
                etEmail.getText()
                        .toString()
                        .trim();

        String password =
                etPassword.getText()
                        .toString()
                        .trim();

        if (TextUtils.isEmpty(loginValue)) {

            etEmail.setError(
                    "Enter username or email"
            );

            etEmail.requestFocus();

            return;
        }

        if (TextUtils.isEmpty(password)) {

            etPassword.setError(
                    "Enter password"
            );

            etPassword.requestFocus();

            return;
        }

        String foundEmail =
                findEmailForLogin(loginValue);

        if (foundEmail == null ||
                foundEmail.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Account not found. Check your username/email.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        foundEmail =
                foundEmail.trim().toLowerCase();

        SharedPreferences userPrefs =
                getSharedPreferences(
                        "UserPrefs_" + foundEmail,
                        Context.MODE_PRIVATE
                );

        String savedEmail =
                userPrefs.getString(
                        "user_email",
                        ""
                );

        String savedUsername =
                userPrefs.getString(
                        "user_name",
                        ""
                );

        String savedPassword =
                userPrefs.getString(
                        "user_password",
                        ""
                );

        if (TextUtils.isEmpty(savedEmail) &&
                TextUtils.isEmpty(savedUsername)) {

            Toast.makeText(
                    this,
                    "Account not found.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (!savedPassword.equals(password)) {

            Toast.makeText(
                    this,
                    "Incorrect password.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        SharedPreferences globalPrefs =
                getSharedPreferences(
                        "UserGlobalPrefs",
                        Context.MODE_PRIVATE
                );

        globalPrefs.edit()
                .putBoolean(
                        "is_logged_in",
                        true
                )
                .putString(
                        "logged_in_email",
                        foundEmail
                )
                .apply();

        Toast.makeText(
                this,
                "Login successful!",
                Toast.LENGTH_SHORT
        ).show();

        openMainActivity(foundEmail);
    }

    // =========================================================
    // FIND EMAIL
    // =========================================================

    private String findEmailForLogin(String value) {

        String cleanValue =
                value.trim();

        if (cleanValue.isEmpty()) {
            return null;
        }

        if (cleanValue.contains("@")) {

            return cleanValue.toLowerCase();
        }

        SharedPreferences indexPrefs =
                getSharedPreferences(
                        "UserAccountIndex",
                        Context.MODE_PRIVATE
                );

        String email =
                indexPrefs.getString(
                        cleanValue.toLowerCase(),
                        ""
                );

        if (!email.isEmpty()) {
            return email;
        }

        return null;
    }

    // =========================================================
    // OPEN MAIN ACTIVITY
    // =========================================================

    private void openMainActivity(String email) {

        if (email == null ||
                email.trim().isEmpty()) {

            return;
        }

        Intent intent =
                new Intent(
                        LoginActivity.this,
                        com.example.walletwise.MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }
}