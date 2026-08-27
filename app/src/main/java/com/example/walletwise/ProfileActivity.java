package com.example.walletwise;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;

public class ProfileActivity extends AppCompatActivity {

    // =========================================================
    // VIEWS
    // =========================================================

    private ImageView btnBack;

    private ShapeableImageView imgProfileAvatar;

    private TextView tvHeaderName;
    private TextView tvProfileRole;

    private EditText etProfileName;
    private EditText etProfileEmail;
    private EditText etProfilePassword;

    private Button btnEditProfile;
    private Button btnLogout;

    private FloatingActionButton btnChangeAvatar;

    // =========================================================
    // IMAGE PICKER
    // =========================================================

    private ActivityResultLauncher<String[]> imagePickerLauncher;

    // =========================================================
    // USER
    // =========================================================

    private String currentEmail = "";

    private SharedPreferences userPrefs;
    private SharedPreferences globalPrefs;

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        // =====================================================
        // FIND VIEWS
        // =====================================================

        btnBack = findViewById(R.id.btnBack);

        imgProfileAvatar =
                findViewById(R.id.imgProfileAvatar);

        tvHeaderName =
                findViewById(R.id.tvHeaderName);

        tvProfileRole =
                findViewById(R.id.tvProfileRole);

        etProfileName =
                findViewById(R.id.etProfileName);

        etProfileEmail =
                findViewById(R.id.etProfileEmail);

        etProfilePassword =
                findViewById(R.id.etProfilePassword);

        btnEditProfile =
                findViewById(R.id.btnEditProfile);

        btnLogout =
                findViewById(R.id.btnLogout);

        btnChangeAvatar =
                findViewById(R.id.btnChangeAvatar);

        // =====================================================
        // IMAGE PICKER
        // =====================================================

        imagePickerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.OpenDocument(),
                        uri -> {

                            if (uri == null) {
                                return;
                            }

                            // -------------------------------------------------
                            // Keep permission to access selected image
                            // -------------------------------------------------

                            try {

                                getContentResolver()
                                        .takePersistableUriPermission(
                                                uri,
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        );

                            } catch (SecurityException ignored) {
                                // Some gallery apps may not provide
                                // persistable permission.
                            }

                            // -------------------------------------------------
                            // Show selected image
                            // -------------------------------------------------

                            if (imgProfileAvatar != null) {

                                imgProfileAvatar.setImageURI(uri);
                            }

                            // -------------------------------------------------
                            // Save image URI for current user
                            // -------------------------------------------------

                            if (userPrefs != null) {

                                userPrefs.edit()
                                        .putString(
                                                "profile_image_uri",
                                                uri.toString()
                                        )
                                        .apply();
                            }

                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Profile picture updated!",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                );

        // =====================================================
        // LOAD CURRENT USER
        // =====================================================

        loadCurrentUser();

        // =====================================================
        // LOAD PROFILE
        // =====================================================

        loadProfile();

        // =====================================================
        // BACK
        // =====================================================

        if (btnBack != null) {

            btnBack.setOnClickListener(v ->
                    finish()
            );
        }

        // =====================================================
        // EDIT PROFILE
        // =====================================================

        if (btnEditProfile != null) {

            btnEditProfile.setOnClickListener(v ->
                    saveProfile()
            );
        }

        // =====================================================
        // LOGOUT
        // =====================================================

        if (btnLogout != null) {

            btnLogout.setOnClickListener(v ->
                    showLogoutConfirmation()
            );
        }

        // =====================================================
        // CHANGE PROFILE PICTURE
        // =====================================================

        if (btnChangeAvatar != null) {

            btnChangeAvatar.setOnClickListener(v -> {

                imagePickerLauncher.launch(
                        new String[]{"image/*"}
                );

            });
        }
    }

    // =========================================================
    // LOAD CURRENT USER
    // =========================================================

    private void loadCurrentUser() {

        globalPrefs =
                getSharedPreferences(
                        "UserGlobalPrefs",
                        Context.MODE_PRIVATE
                );

        currentEmail =
                globalPrefs.getString(
                        "logged_in_email",
                        ""
                );

        if (currentEmail == null) {
            currentEmail = "";
        }

        currentEmail =
                currentEmail.trim().toLowerCase();

        // -----------------------------------------------------
        // NO LOGGED-IN USER
        // -----------------------------------------------------

        if (currentEmail.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please login first.",
                    Toast.LENGTH_SHORT
            ).show();

            goToLogin();

            return;
        }

        // -----------------------------------------------------
        // USER-SPECIFIC PREFS
        // -----------------------------------------------------

        userPrefs =
                getSharedPreferences(
                        "UserPrefs_" + currentEmail,
                        Context.MODE_PRIVATE
                );
    }

    // =========================================================
    // LOAD PROFILE
    // =========================================================

    private void loadProfile() {

        if (userPrefs == null ||
                currentEmail.isEmpty()) {

            return;
        }

        // -----------------------------------------------------
        // USER NAME
        // -----------------------------------------------------

        String name =
                userPrefs.getString(
                        "user_name",
                        "User"
                );

        // -----------------------------------------------------
        // EMAIL
        // -----------------------------------------------------

        String email =
                userPrefs.getString(
                        "user_email",
                        currentEmail
                );

        // -----------------------------------------------------
        // ROLE
        // -----------------------------------------------------

        String role =
                userPrefs.getString(
                        "user_role",
                        "Student"
                );

        // -----------------------------------------------------
        // PASSWORD
        // -----------------------------------------------------

        String password =
                userPrefs.getString(
                        "user_password",
                        ""
                );

        // -----------------------------------------------------
        // PROFILE IMAGE
        // -----------------------------------------------------

        String profileImageUri =
                userPrefs.getString(
                        "profile_image_uri",
                        ""
                );

        if (!profileImageUri.isEmpty() &&
                imgProfileAvatar != null) {

            try {

                imgProfileAvatar.setImageURI(
                        Uri.parse(profileImageUri)
                );

            } catch (Exception ignored) {
                // If saved image is no longer available,
                // the default avatar remains visible.
            }
        }

        // =====================================================
        // HEADER
        // =====================================================

        if (tvHeaderName != null) {

            tvHeaderName.setText(name);
        }

        if (tvProfileRole != null) {

            tvProfileRole.setText(role);
        }

        // =====================================================
        // NAME
        // =====================================================

        if (etProfileName != null) {

            etProfileName.setText(name);
        }

        // =====================================================
        // EMAIL
        // =====================================================

        if (etProfileEmail != null) {

            etProfileEmail.setText(email);

            // Email is the account identifier.
            // Keep it non-editable.

            etProfileEmail.setEnabled(false);

            etProfileEmail.setTextColor(Color.GRAY);
        }

        // =====================================================
        // PASSWORD
        // =====================================================

        if (etProfilePassword != null) {

            etProfilePassword.setInputType(
                    InputType.TYPE_CLASS_TEXT |
                            InputType.TYPE_TEXT_VARIATION_PASSWORD
            );

            etProfilePassword.setText(password);
        }
    }

    // =========================================================
    // SAVE PROFILE
    // =========================================================

    private void saveProfile() {

        if (userPrefs == null ||
                currentEmail.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please login again.",
                    Toast.LENGTH_SHORT
            ).show();

            goToLogin();

            return;
        }

        // -----------------------------------------------------
        // GET NAME
        // -----------------------------------------------------

        String newName = "";

        if (etProfileName != null) {

            newName =
                    etProfileName
                            .getText()
                            .toString()
                            .trim();
        }

        // -----------------------------------------------------
        // GET PASSWORD
        // -----------------------------------------------------

        String newPassword = "";

        if (etProfilePassword != null) {

            newPassword =
                    etProfilePassword
                            .getText()
                            .toString()
                            .trim();
        }

        // =====================================================
        // VALIDATE NAME
        // =====================================================

        if (TextUtils.isEmpty(newName)) {

            if (etProfileName != null) {

                etProfileName.setError(
                        "Enter your name"
                );

                etProfileName.requestFocus();
            }

            return;
        }

        // =====================================================
        // VALIDATE PASSWORD
        // =====================================================

        if (TextUtils.isEmpty(newPassword)) {

            if (etProfilePassword != null) {

                etProfilePassword.setError(
                        "Enter your password"
                );

                etProfilePassword.requestFocus();
            }

            return;
        }

        // =====================================================
        // SAVE
        // =====================================================

        userPrefs.edit()
                .putString(
                        "user_name",
                        newName
                )
                .putString(
                        "user_password",
                        newPassword
                )
                .apply();

        // =====================================================
        // UPDATE HEADER
        // =====================================================

        if (tvHeaderName != null) {

            tvHeaderName.setText(newName);
        }

        Toast.makeText(
                this,
                "Profile updated successfully!",
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================================================
    // LOGOUT CONFIRMATION
    // =========================================================

    private void showLogoutConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage(
                        "Are you sure you want to logout?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Logout",
                        (dialog, which) ->
                                logout()
                )
                .show();
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private void logout() {

        /*
         * User data is NOT deleted.
         *
         * UserPrefs_<email>
         * Data_<email>
         * Transactions
         * Budget
         * Profile picture
         *
         * all remain safe.
         *
         * Only the active login session is removed.
         */

        SharedPreferences prefs =
                getSharedPreferences(
                        "UserGlobalPrefs",
                        Context.MODE_PRIVATE
                );

        prefs.edit()
                .putBoolean(
                        "is_logged_in",
                        false
                )
                .remove(
                        "logged_in_email"
                )
                .apply();

        Toast.makeText(
                this,
                "Logged out successfully.",
                Toast.LENGTH_SHORT
        ).show();

        goToLogin();
    }

    // =========================================================
    // GO TO LOGIN
    // =========================================================

    private void goToLogin() {

        Intent intent =
                new Intent(
                        ProfileActivity.this,
                        LoginActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }
}