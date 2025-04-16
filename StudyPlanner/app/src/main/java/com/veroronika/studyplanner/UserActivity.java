package com.veroronika.studyplanner;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Objects;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;

public class UserActivity extends AppCompatActivity {
    Button accountsettingbutton, notificationsettingbutton, deleteaccountbutton;
    FirebaseAuth auth;
    FirebaseUser user;
    ImageView userprofilepicture;
    TextView studystreaks, diarystreaks, username;
    BottomNavigationView bottomNavigationView;

    private static MediaPlayer buttonSound;
    private static MediaPlayer buttonSaveSound;
    private static MediaPlayer buttonCancelSound;


    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_user);

        username = findViewById(R.id.username);
        accountsettingbutton = findViewById(R.id.accountsettingbutton);
        notificationsettingbutton = findViewById(R.id.notificationsettingbutton);
        deleteaccountbutton = findViewById(R.id.deleteaccountbutton);
        userprofilepicture = findViewById(R.id.userprofilepicture);
        bottomNavigationView = findViewById(R.id.bottomnavigationview);
        studystreaks = findViewById(R.id.studystreaks);
        diarystreaks = findViewById(R.id.diarystreaks);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);

        buttonSound = MediaPlayer.create(this, R.raw.button_click);
        buttonSaveSound = MediaPlayer.create(this, R.raw.savebutton_click);
        buttonCancelSound = MediaPlayer.create(this, R.raw.cancelbutton_click);

        setupBottomNavigation();
        loadUserData();
        setupButtons();
    }

    @Override
    protected void onStart() {
        super.onStart();

        int selectedItemId = R.id.navigation_user;
        bottomNavigationView.setSelectedItemId(selectedItemId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (buttonSound != null) buttonSound.release();
        if (buttonSaveSound != null) buttonSaveSound.release();
        if (buttonCancelSound != null) buttonCancelSound.release();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_user);
        bottomNavigationView.setItemActiveIndicatorColor(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.main_default_dark))
        );

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (Objects.requireNonNull(item.getTitle()).toString()) {
                case "Home":
                    playButtonSound(UserActivity.this);
                    navigateTo(MainActivity.class);
                    return true;
                case "Study":
                    playButtonSound(UserActivity.this);
                    navigateTo(StudyActivity.class);
                    return true;
                case "Diary":
                    playButtonSound(UserActivity.this);
                    navigateTo(DiaryActivity.class);
                    return true;
                case "User":
                    playButtonSound(UserActivity.this);
                    return true;
                default:
                    return false;
            }
        });
    }

    private void loadUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        studystreaks.setText("Study: " + sharedPreferences.getInt("studyStreak", 0) + " days");
        diarystreaks.setText("Diary: " + sharedPreferences.getInt("diaryStreak", 0) + " days");

        if (user != null && user.getDisplayName() != null) {
            String displayName = user.getDisplayName();
            username.setText(displayName);
            username.setTextSize(displayName.length() > 5 ? 14 : 16);
        } else {
            username.setText("User");
            username.setTextSize(16);
        }
    }

    private void setupButtons() {
        deleteaccountbutton.setOnClickListener(v -> deleteAccount());
        accountsettingbutton.setOnClickListener(v -> accountsettingbuttondialog());
        notificationsettingbutton.setOnClickListener(v -> showSettingsDialog());

        userprofilepicture.setOnClickListener(v -> {
            playButtonSound(UserActivity.this);
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });
    }

    private void deleteAccount() {
        playCancelSound(UserActivity.this);
        if (user != null) {
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Account deletion successful.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Account deletion failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(this, "No user logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void accountsettingbuttondialog() {
        playButtonSound(UserActivity.this);
        if (user == null) {
            Toast.makeText(this, "No user is logged in. Please log in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_account_settings);

        EditText editTextDisplayName = dialog.findViewById(R.id.editTextDisplayName);
        EditText editTextPassword = dialog.findViewById(R.id.editTextPassword);
        EditText editTextNewPassword = dialog.findViewById(R.id.editTextNewPassword);
        Button buttonUpdateDisplayName = dialog.findViewById(R.id.buttonUpdateDisplayName);
        Button buttonUpdatePassword = dialog.findViewById(R.id.buttonUpdatePassword);
        ProgressBar progressBar = dialog.findViewById(R.id.progressBar);
        Button logOut = dialog.findViewById(R.id.logOut);
        Button cancel = dialog.findViewById(R.id.cancel_button);

        logOut.setOnClickListener(v -> {
            playCancelSound(UserActivity.this);
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            navigateTo(MainActivity.class);
        });

        cancel.setOnClickListener(v -> {
            playCancelSound(this);
            dialog.dismiss();
        });

        buttonUpdateDisplayName.setOnClickListener(v -> updateDisplayName(editTextDisplayName, progressBar));
        buttonUpdatePassword.setOnClickListener(v -> updatePassword(editTextPassword, editTextNewPassword, progressBar));

        dialog.show();
    }

    private void showSettingsDialog() {
        playButtonSound(UserActivity.this);
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_settings);

        Switch switchNotifications = dialog.findViewById(R.id.switch_notifications);
        Button saveButton = dialog.findViewById(R.id.save_button);
        Button cancelButton = dialog.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        switchNotifications.setChecked(true);
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            playButtonSound(UserActivity.this);
            SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("notificationsEnabled", isChecked);
            editor.apply();

            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "Notifications " + status, Toast.LENGTH_SHORT).show();
        });

        saveButton.setOnClickListener(v -> {
            playSaveSound(this);
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v ->{
            playCancelSound(this);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void updateDisplayName(EditText editTextDisplayName, ProgressBar progressBar) {
        playSaveSound(UserActivity.this);
        String displayName = editTextDisplayName.getText().toString().trim();
        if (TextUtils.isEmpty(displayName)) {
            Toast.makeText(this, "Enter a display name", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                playSaveSound(UserActivity.this);
                Toast.makeText(this, "Display name updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update display name", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePassword(EditText editTextPassword, EditText editTextNewPassword, ProgressBar progressBar) {
        playButtonSound(UserActivity.this);
        String oldPassword = editTextPassword.getText().toString().trim();
        String newPassword = editTextNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(oldPassword) || TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "Please fill in all password fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful() && user.isEmailVerified()) {
                    AuthCredential credential = EmailAuthProvider.getCredential(Objects.requireNonNull(user.getEmail()), oldPassword);
                    user.reauthenticate(credential).addOnCompleteListener(authTask -> {
                        if (authTask.isSuccessful()) {
                            user.updatePassword(newPassword).addOnCompleteListener(passwordTask -> {
                                progressBar.setVisibility(View.GONE);
                                if (passwordTask.isSuccessful()) {
                                    playSaveSound(UserActivity.this);
                                    Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Re-authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Please verify your email before changing the password.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(UserActivity.this, targetActivity);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public static void playButtonSound(Context context) {
        Log.d("Mediaplayer", "Playing a sound");
    }

    public static void playSaveSound(Context context) {
        Log.d("Mediaplayer", "Playing a sound");
    }

    public static void playCancelSound(Context context) {
        Log.d("Mediaplayer", "Playing a sound");
    }
}