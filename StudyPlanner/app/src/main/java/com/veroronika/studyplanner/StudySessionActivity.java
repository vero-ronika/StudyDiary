package com.veroronika.studyplanner;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.veroronika.studyplanner.database.DatabaseHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.content.Context;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class StudySessionActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;

    private TextView timerTextView;
    private int seconds = 0;
    private boolean isRunning = true;
    private Handler handler;

    FirebaseAuth mAuth;
    String userId;

    public boolean firebaseuserboolean;
    private DatabaseHelper databaseHelper;

    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_session);

        ImageView gifImageView = findViewById(R.id.gifImageView);
        Glide.with(this).asGif().load(R.drawable.catstudying).into(gifImageView);

        FirebaseApp.initializeApp(this);

        databaseHelper = new DatabaseHelper(this);
        mAuth = FirebaseAuth.getInstance();

        TextView typeHereWindow = findViewById(R.id.typeherewindow);
        TextView subjectNameView = findViewById(R.id.subjectNameTextView);
        timerTextView = findViewById(R.id.timerTextView);
        Button saveButton = findViewById(R.id.saveButton);
        Button cancelButton = findViewById(R.id.cancelButton);
        stopButton = findViewById(R.id.stopButton);

        Intent intent = getIntent();
        String subjectName = intent.getStringExtra("subjectName");
        int tagIndex = intent.getIntExtra("tagIndex", -1);

        subjectNameView.setText(subjectName != null && !subjectName.isEmpty() ? subjectName : "Subject not selected");

        SharedPreferences sharedPreferences = getSharedPreferences("study_preferences", MODE_PRIVATE);
        String textStudy = sharedPreferences.getString("text_study", "No additional notes");
        typeHereWindow.setText(textStudy);
        startTimer();

        saveButton.setOnClickListener(v -> {
            UserActivity.playSaveSound(this);
            stopTimer();
            saveStudySession(subjectName, tagIndex, textStudy);
            Toast.makeText(StudySessionActivity.this, "Study session saved!", Toast.LENGTH_SHORT).show();

            Intent studyIntent = new Intent(StudySessionActivity.this, StudyActivity.class);
            startActivity(studyIntent);
        });

        stopButton.setOnClickListener(v -> {
            UserActivity.playButtonSound(this);

            if (!isRunning) {
                isRunning = true;
                stopButton.setText("Stop");
                startTimer();
                Glide.with(this).asGif().load(R.drawable.catstudying).into(gifImageView);
            } else {
                stopButton.setText("Start");
                stopTimer();
                gifImageView.setImageResource(R.drawable.lastframe);
            }
        });


        cancelButton.setOnClickListener(v -> {
            UserActivity.playCancelSound(this);
            Intent cancelintent = new Intent(StudySessionActivity.this, StudyActivity.class);
            startActivity(cancelintent);
        });

        bottomNavigationView = findViewById(R.id.bottomnavigationview);
        ColorStateList colorStateList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.main_default)
        );
        bottomNavigationView.setItemActiveIndicatorColor(colorStateList);

        setupBottomNavigationView();

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();

            if (isConnectedToInternet()) {
                if (!firebaseuserboolean) {
                    firebaseuserboolean = true;
                }
            } else {
                firebaseuserboolean = false;
            }
        } else {
            firebaseuserboolean = false;
        }
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent;
            switch (Objects.requireNonNull(item.getTitle()).toString()) {
                case "Home":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(StudySessionActivity.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;

                case "Study":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(StudySessionActivity.this, StudyActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;

                case "Diary":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(StudySessionActivity.this, DiaryActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;

                case "User":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(StudySessionActivity.this, UserActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                default:
                    return false;
            }
        });
    }

    private boolean isConnectedToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void startTimer() {
        handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    int hours = seconds / 3600;
                    int minutes = (seconds % 3600) / 60;
                    int secs = seconds % 60;

                    String time = String.format("%02d:%02d:%02d", hours, minutes, secs);
                    timerTextView.setText(time);
                    seconds++;
                    handler.postDelayed(this, 1000);
                }
            }
        });
    }

    private void stopTimer() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void saveStudySession(String subjectName, int tagIndex, String text) {
        String currentDate = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        String stopwatchTime = String.format("%02d:%02d:%02d", hours, minutes, secs);

        if (subjectName == null || subjectName.trim().isEmpty() ||
                text == null || text.trim().isEmpty() ||
                tagIndex == -1) {
            Toast.makeText(StudySessionActivity.this, "Please fill in all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("StudySessionActivity", "Saving to database: SubjectName=" + subjectName + ", TagIndex=" + tagIndex + ", StopwatchTime=" + stopwatchTime);

        if (firebaseuserboolean) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("study_sessions").child(userId);
                String sessionId = databaseReference.push().getKey();

                if (sessionId != null) {
                    HashMap<String, Object> studySessionData = new HashMap<>();
                    studySessionData.put("date", currentDate);
                    studySessionData.put("time", currentTime);
                    studySessionData.put("stopwatch_time", stopwatchTime);
                    studySessionData.put("text", text);
                    studySessionData.put("subject_name", subjectName);
                    studySessionData.put("tag_index", tagIndex);

                    databaseReference.child(sessionId).setValue(studySessionData).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("StudySessionActivity", "Study session added to Firebase: " + sessionId);
                        } else {
                            Log.w("StudySessionActivity", "Failed to add study session to Firebase", task.getException());
                        }
                    });
                }
            }
        } else {
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_DATE, currentDate);
            values.put(DatabaseHelper.COLUMN_TIME, currentTime);
            values.put(DatabaseHelper.COLUMN_STOPWATCH_TIME, stopwatchTime);
            values.put(DatabaseHelper.COLUMN_TEXT, text);
            values.put(DatabaseHelper.COLUMN_SUBJECT_NAME_SESSION, subjectName);
            values.put(DatabaseHelper.COLUMN_TAG_INDEX_SESSION, tagIndex);

            long newRowId = db.insert(DatabaseHelper.TABLE_STUDY_SESSION, null, values);

            if (newRowId != -1) {
                Log.d("StudySessionActivity", "Study session saved to SQLite");
            } else {
                Log.w("StudySessionActivity", "Failed to save study session to SQLite");
            }
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }
}
