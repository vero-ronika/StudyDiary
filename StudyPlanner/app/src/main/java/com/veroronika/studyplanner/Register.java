package com.veroronika.studyplanner;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class Register extends AppCompatActivity {
    TextInputEditText editTextEmail, editTextPassword, editTextDisplayName;
    Button buttonReg, backbutton;
    FirebaseAuth mAuth;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        backbutton = findViewById(R.id.backbutton);
        backbutton.setOnClickListener(v -> {
            UserActivity.playCancelSound(this);
            Intent intent = new Intent(Register.this, UserActivity.class);
            startActivity(intent);
        });

        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        editTextDisplayName = findViewById(R.id.display_name);
        buttonReg = findViewById(R.id.btn_register);
        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressBar);

        buttonReg.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            String email = String.valueOf(editTextEmail.getText());
            String password = String.valueOf(editTextPassword.getText());
            String displayName = String.valueOf(editTextDisplayName.getText());

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(Register.this, "Enter email", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(Register.this, "Enter password", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            if (TextUtils.isEmpty(displayName)) {
                Toast.makeText(Register.this, "Enter display name", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(displayName)
                                        .build();

                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(profileTask -> {
                                            if (profileTask.isSuccessful()) {
                                                user.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                                    progressBar.setVisibility(View.GONE);
                                                    if (emailTask.isSuccessful()) {
                                                        Log.d("Register", "Verification email sent successfully.");
                                                        Toast.makeText(Register.this, "Account created successfully. A verification email has been sent. Please check your inbox.", Toast.LENGTH_LONG).show();
                                                        Intent intent = new Intent(getApplicationContext(), Login.class);
                                                        startActivity(intent);
                                                        finish();
                                                    } else {
                                                        Log.e("Register", "Failed to send verification email: " + emailTask.getException());
                                                        Toast.makeText(Register.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            } else {
                                                progressBar.setVisibility(View.GONE);
                                                Log.e("Register", "Failed to set display name: " + profileTask.getException());
                                                Toast.makeText(Register.this, "Failed to set display name.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Exception exception = task.getException();
                            if (exception != null && exception.getMessage() != null && exception.getMessage().contains("email address is already in use")) {
                                Toast.makeText(Register.this, "This email is already registered. Please use a different one or log in.", Toast.LENGTH_LONG).show();
                            } else {
                                Log.e("Register", "Authentication failed: " + exception);
                                Toast.makeText(Register.this, "Authentication failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });
    }
}
