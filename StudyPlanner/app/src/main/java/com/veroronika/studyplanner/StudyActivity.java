package com.veroronika.studyplanner;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.veroronika.studyplanner.database.DatabaseHelper;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class StudyActivity extends AppCompatActivity {
    public boolean firebaseuserboolean;
    private DatabaseHelper databaseHelper;

    private HorizontalBarChart barChart;

    private int tagIndex = -1;
    private String selectedDate;
    private String reminderTime;

    private RecyclerView studyRecyclerView;
    private StudyAdapter studyAdapter;
    private List<String[]> studyList = new ArrayList<>();

    private List<String> dateList;

    private static MediaPlayer buttonSound;
    private static MediaPlayer buttonSaveSound;
    private static MediaPlayer buttonCancelSound;
    private static float soundVolume = 1.0f;

    static final int[] tags = {
            R.drawable.tag_lightred,
            R.drawable.tag_lightorange,
            R.drawable.tag_lightyellow,
            R.drawable.tag_lightgreen,
            R.drawable.tag_lightblue,
            R.drawable.tag_lightpurple,
            R.drawable.tag_darkred,
            R.drawable.tag_darkorange,
            R.drawable.tag_darkyellow,
            R.drawable.tag_darkgreen,
            R.drawable.tag_darkblue,
            R.drawable.tag_darkpurple
    };

    Button startstudybutton, subjectchangebutton, addreminderbutton;
    TextView tag_selection_name, nostudyTextView, dots, subjectstudywindow, currentstreakstudy, noremindersTextView, datereminder1, textreminder1, timereminder1, datereminder2, timereminder2, textreminder2, datereminder3, timereminder3, textreminder3, progressionsubjectsTextView;
    ImageView reminderImageView1, reminderImageView2, reminderImageView3;
    ConstraintLayout reminderentry1, reminderentry2, reminderentry3;

    String userId;

    BarChart progressionstudyTable;
    BottomNavigationView bottomNavigationView;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);

        databaseHelper = new DatabaseHelper(this);

        setContentView(R.layout.activity_study);

        mAuth = FirebaseAuth.getInstance();

        barChart = findViewById(R.id.progressionsubjectsTable);

        progressionstudyTable = findViewById(R.id.progressionstudyTable);


        studyRecyclerView = findViewById(R.id.studyRecyclerView);
        if (studyRecyclerView == null) {
            Log.e("StudyActivity", "RecyclerView not found!");
        }

        nostudyTextView = findViewById(R.id.nostudyTextView);

        studyRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        studyRecyclerView.setAdapter(new StudyAdapter(studyList, tags));

        addreminderbutton = findViewById(R.id.addreminderbutton);
        dots = findViewById(R.id.dots);
        subjectchangebutton = findViewById(R.id.subjectchangebutton);

        subjectstudywindow = findViewById(R.id.subjectstudywindow);

        startstudybutton = findViewById(R.id.startstudybutton);

        currentstreakstudy = findViewById(R.id.currentstreakstudy);
        datereminder1 = findViewById(R.id.datereminder1);
        timereminder1 = findViewById(R.id.timereminder1);
        textreminder1 = findViewById(R.id.textreminder1);
        reminderImageView1 = findViewById(R.id.reminderImageView1);
        datereminder2 = findViewById(R.id.datereminder2);
        timereminder2 = findViewById(R.id.timereminder2);
        textreminder2 = findViewById(R.id.textreminder2);
        reminderImageView2 = findViewById(R.id.reminderImageView2);
        datereminder3 = findViewById(R.id.datereminder3);
        timereminder3 = findViewById(R.id.timereminder3);
        textreminder3 = findViewById(R.id.textreminder3);
        reminderImageView3 = findViewById(R.id.reminderImageView3);

        reminderentry1 = findViewById(R.id.reminderentry1);
        reminderentry2 = findViewById(R.id.reminderentry2);
        reminderentry3 = findViewById(R.id.reminderentry3);

        noremindersTextView = findViewById(R.id.noremindersTextView);

        progressionsubjectsTextView = findViewById(R.id.progressionsubjectsTextView);

        bottomNavigationView = findViewById(R.id.bottomnavigationview);
        int selectedItemId = R.id.navigation_study;

        bottomNavigationView.setSelectedItemId(selectedItemId);
        ColorStateList colorStateList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.main_default_dark)
        );
        bottomNavigationView.setItemActiveIndicatorColor(colorStateList);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent;
            switch (Objects.requireNonNull(item.getTitle()).toString()) {
                case "Home":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(StudyActivity.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;

                case "Study":
                    UserActivity.playButtonSound(this);
                    return true;

                case "Diary":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(StudyActivity.this, DiaryActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;

                case "User":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(StudyActivity.this, UserActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                default:
                    return false;
            }
        });

        subjectchangebutton.setOnClickListener(v -> subjectchangebuttondialog());

        addreminderbutton.setOnClickListener(v -> reminderbuttondialog());

        dots.setOnClickListener(v -> {
            UserActivity.playButtonSound(this);
            Intent intent = new Intent(StudyActivity.this, AllRemindersActivity.class);
            startActivity(intent);
        });

        setupReminderEntryListeners();

        startstudybutton.setOnClickListener(v -> {
            UserActivity.playButtonSound(this);
            SharedPreferences sharedPreferences = getSharedPreferences("study_preferences", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            String subjectName = sharedPreferences.getString("subjectName", "");
            int tagIndex = sharedPreferences.getInt("tagIndex", -1);

            EditText typeHereInput = findViewById(R.id.typeherewindow);
            String textStudy = typeHereInput.getText().toString().trim();

            if (!textStudy.isEmpty()) {
                editor.putString("text_study", textStudy);
                editor.apply();
            }

            if (!subjectName.isEmpty() && tagIndex != -1) {
                Intent intent = new Intent(StudyActivity.this, StudySessionActivity.class);
                intent.putExtra("subjectName", subjectName);
                intent.putExtra("tagIndex", tagIndex);
                startActivity(intent);
            } else {
                Toast.makeText(StudyActivity.this, "Please select a subject first.", Toast.LENGTH_SHORT).show();
            }
        });

        boolean callReminderDialog = getIntent().getBooleanExtra("CALL_REMINDER_DIALOG", false);
        if (callReminderDialog) {
            reminderbuttondialog();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        int selectedItemId = R.id.navigation_study;

        bottomNavigationView.setSelectedItemId(selectedItemId);

        createNotificationChannel(this);

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();

            if (isConnectedToInternet()) {
                firebaseuserboolean = true;
                migrateDataToFirebase();
                migrateStudySessionDataToFirebase();
                migrateSubjectNamesToFirebase();

                loadRemindersFromFirebase();
                loadStudySessionsFromFirebase();
            } else {
                firebaseuserboolean = false;
                loadRemindersFromSQL();
                loadStudySessionsFromSQL();
            }
        } else {
            firebaseuserboolean = false;
            loadRemindersFromSQL();
            loadStudySessionsFromSQL();
        }
        getDataProgressSubjects();
    }

    private void scheduleNotification(String textReminder, String dateReminder, String timeReminder, String subjectName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    Log.e("Notification", "Permission to schedule exact alarms is not granted.");
                    return;
                }
            }

            String dateTimeString = dateReminder + " " + timeReminder;
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            Date date = dateFormat.parse(dateTimeString);
            if (date == null) {
                Log.e("Notification", "Failed to parse date or time for reminder.");
                return;
            }

            long scheduledTime = date.getTime();
            long currentTime = System.currentTimeMillis();

            if (scheduledTime <= currentTime) {
                Log.d("NotificationScheduler", "Scheduled time has passed, not scheduling the notification.");
                return;
            }

            Log.d("NotificationScheduler", "Notification scheduled for: " + scheduledTime);

            if (!isConnectedToInternet()) {
                Log.d("NotificationScheduler", "No internet connection, scheduling offline notification.");
                Intent intent = new Intent(this, NotificationReceiver.class);
                intent.putExtra("textReminder", textReminder);
                intent.putExtra("subjectName", subjectName);

                int notificationId = subjectName.hashCode();

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        notificationId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                AlarmManager alarmManagerOffline = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManagerOffline != null) {
                    alarmManagerOffline.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledTime, pendingIntent);
                    Log.d("NotificationScheduler", "Offline notification scheduled at: " + date);
                }
            } else {
                Log.d("NotificationScheduler", "Internet connection is available, scheduling notification.");
                scheduleNotificationOnline(textReminder, dateReminder, timeReminder, subjectName);
            }

        } catch (ParseException e) {
            Log.e("NotificationScheduler", "Failed to parse the date or time for reminder: " + e.getMessage());
        } catch (SecurityException e) {
            Log.e("NotificationScheduler", "SecurityException: Permission to schedule exact alarms denied.");
        }
    }

    private void scheduleNotificationOnline(String textReminder, String dateReminder, String timeReminder, String subjectName) {
        try {
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    Log.e("Notification", "Permission to schedule exact alarms is not granted.");
                    
                    return;
                }
            }

            String dateTimeString = dateReminder + " " + timeReminder;
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            Date date = dateFormat.parse(dateTimeString);
            if (date == null) {
                Log.e("NotificationScheduler", "Failed to parse date or time for reminder.");
                return;
            }

            long scheduledTime = date.getTime();
            long currentTime = System.currentTimeMillis();

            if (scheduledTime <= currentTime) {
                Log.d("NotificationScheduler", "Scheduled time has passed, not scheduling the notification.");
                return;
            }

            Intent intent = new Intent(this, NotificationReceiver.class);
            intent.putExtra("textReminder", textReminder);
            intent.putExtra("subjectName", subjectName);

            int notificationId = subjectName.hashCode();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManagerOnline = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManagerOnline != null) {
                alarmManagerOnline.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledTime, pendingIntent);
                Log.d("NotificationScheduler", "Notification scheduled at: " + date);
            }

        } catch (ParseException e) {
            Log.e("NotificationScheduler", "Failed to parse the date or time for reminder: " + e.getMessage());
        } catch (SecurityException e) {
            Log.e("NotificationScheduler", "SecurityException: Permission to schedule exact alarms denied.");
        }
    }

    private void createNotificationChannel(Context context) {
        CharSequence name = "Study Notifications";
        String description = "Channel for study reminders";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel("study_channel", name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void calculateStreak() {
        int streak = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        if (dateList == null || dateList.isEmpty()) {
            currentstreakstudy.setText("Current streak: 0 days");
            return;
        }
        Set<String> dateSet = new HashSet<>(dateList);

        try {
            Calendar calendar = Calendar.getInstance();
            Date today = calendar.getTime();

            while (dateSet.contains(sdf.format(today))) {
                streak++;
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                today = calendar.getTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        currentstreakstudy.setText("Current streak: " + streak + " days");
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("studyStreak", streak);
        editor.apply();
    }


    private void updateReminderImageView(ImageView imageView, int tagIndex, String subjectName) {
        
        if (tagIndex >= 0 && tagIndex < tags.length) {
            imageView.setImageResource(tags[tagIndex]);
            imageView.setContentDescription(subjectName + " tag");
        } else {
            Log.e("TAG_SELECTION", "Invalid tagIndex: " + tagIndex);
        }
    }



    private boolean isConnectedToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void checkAndUpdateSubjectNameInFirebase(String userId, int tagIndex, String subjectName) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("subjectnames").child(userId);

        
        databaseReference.orderByChild("tagIndex").equalTo(tagIndex)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            snapshot.getRef().removeValue(); 
                            Log.d("Firebase", "Deleted old subject with tagIndex: " + tagIndex);
                        }

                        
                        String subjectNameKey = databaseReference.push().getKey();
                        if (subjectNameKey != null) {
                            HashMap<String, Object> subjectMap = new HashMap<>();
                            subjectMap.put("subjectName", subjectName);
                            subjectMap.put("tagIndex", tagIndex);

                            databaseReference.child(subjectNameKey).setValue(subjectMap)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Log.d("Firebase", "Added new subject: " + subjectName);
                                        } else {
                                            Log.w("Firebase", "Failed to add subject", task.getException());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w("Firebase", "Failed to check subject name", databaseError.toException());
                    }
                });
    }

    private void migrateSubjectNamesToFirebase() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String[] projection = {
                DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT,
                DatabaseHelper.COLUMN_SUBJECT_NAME_TAG
        };

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_SUBJECT_NAMES,
                projection,
                null, null, null, null, null, null
        );

        if (cursor.moveToFirst()) {
            do {
                int tagIndexColumn = cursor.getColumnIndex(DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT);
                int subjectNameColumn = cursor.getColumnIndex(DatabaseHelper.COLUMN_SUBJECT_NAME_TAG);

                if (tagIndexColumn >= 0 && subjectNameColumn >= 0) {
                    int tagIndex = cursor.getInt(tagIndexColumn);
                    String subjectName = cursor.getString(subjectNameColumn);

                    if (firebaseuserboolean) {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            String userId = currentUser.getUid();

                            checkAndUpdateSubjectNameInFirebase(userId, tagIndex, subjectName);

                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("subjectnames").child(userId);

                            
                            databaseReference.orderByChild("tagIndex").equalTo(tagIndex)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                snapshot.getRef().removeValue();
                                            }

                                            
                                            String subjectNameKey = databaseReference.push().getKey();
                                            if (subjectNameKey != null) {
                                                HashMap<String, Object> subjectMap = new HashMap<>();
                                                subjectMap.put("subjectName", subjectName);
                                                subjectMap.put("tagIndex", tagIndex);

                                                databaseReference.child(subjectNameKey).setValue(subjectMap).addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        Log.d("SubjectMigration", "Subject name entry added to Firebase: " + subjectNameKey);
                                                    } else {
                                                        Log.w("SubjectMigration", "Failed to add subject name to Firebase", task.getException());
                                                    }
                                                });
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            Log.w("SubjectMigration", "Firebase query cancelled", databaseError.toException());
                                        }
                                    });
                        }
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        SQLiteDatabase writableDb = databaseHelper.getWritableDatabase();
        int deletedRows = writableDb.delete(DatabaseHelper.TABLE_SUBJECT_NAMES, null, null);
        Log.d("SubjectMigration", "Deleted " + deletedRows + " entries from local SQLite after migration.");
    }

    private void migrateStudySessionDataToFirebase() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String[] projection = {
                DatabaseHelper.COLUMN_DATE,
                DatabaseHelper.COLUMN_TIME,
                DatabaseHelper.COLUMN_STOPWATCH_TIME,
                DatabaseHelper.COLUMN_TEXT,
                DatabaseHelper.COLUMN_SUBJECT_NAME_SESSION,
                DatabaseHelper.COLUMN_TAG_INDEX_SESSION,
        };

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_STUDY_SESSION,
                projection,
                null, null, null, null, null, null
        );

        if (cursor.moveToFirst()) {
            do {
                int dateIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE);
                int timeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME);
                int stopwatchTimeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_STOPWATCH_TIME);
                int textIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT);
                int subjectNameIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SUBJECT_NAME_SESSION);
                int tagIndexSessionIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TAG_INDEX_SESSION);

                if (dateIndex >= 0 && timeIndex >= 0 && stopwatchTimeIndex >= 0 && textIndex >= 0 && subjectNameIndex >= 0 && tagIndexSessionIndex >= 0) {
                    String date = cursor.getString(dateIndex);
                    String time = cursor.getString(timeIndex);
                    String stopwatchTime = cursor.getString(stopwatchTimeIndex);
                    String text = cursor.getString(textIndex);
                    String subjectName = cursor.getString(subjectNameIndex);
                    int tagIndex = cursor.getInt(tagIndexSessionIndex);

                    if (firebaseuserboolean) {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            String userId = currentUser.getUid();

                            checkAndUpdateSubjectNameInFirebase(userId, tagIndex, subjectName);

                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("study_sessions").child(userId);
                            String sessionId = databaseReference.push().getKey();

                            if (sessionId != null) {
                                HashMap<String, Object> studySessionData = new HashMap<>();
                                studySessionData.put("date", date);
                                studySessionData.put("time", time);
                                studySessionData.put("stopwatch_time", stopwatchTime);
                                studySessionData.put("text", text);
                                studySessionData.put("subject_name", subjectName);
                                studySessionData.put("tag_index", tagIndex);

                                databaseReference.child(sessionId).setValue(studySessionData).addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        String whereClause = DatabaseHelper.COLUMN_DATE + " = ?";
                                        String[] whereArgs = { date };
                                        db.delete(DatabaseHelper.TABLE_STUDY_SESSION, whereClause, whereArgs);
                                        Log.d("StudySessionActivity", "Study session entry added to Firebase: " + sessionId);
                                    } else {
                                        Log.w("StudySessionActivity", "Failed to add study session to Firebase", task.getException());
                                    }
                                });
                            }
                        }
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
    }



    private void migrateDataToFirebase() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String[] projection = {
                DatabaseHelper.COLUMN_DATE_REMINDER,
                DatabaseHelper.COLUMN_TEXT_REMINDER,
                DatabaseHelper.COLUMN_TAG_INDEX,
                DatabaseHelper.COLUMN_SUBJECT_NAME,
                DatabaseHelper.COLUMN_TIME_REMINDER,
        };

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_STUDY,
                projection,
                null, null, null, null, null, null
        );

        if (cursor.moveToFirst()) {
            do {
                int dateReminderIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE_REMINDER);
                int textReminderIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT_REMINDER);
                int tagIndexColumn = cursor.getColumnIndex(DatabaseHelper.COLUMN_TAG_INDEX);
                int subjectNameIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SUBJECT_NAME);
                int timeReminderIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME_REMINDER);

                if (dateReminderIndex >= 0 && textReminderIndex >= 0 && tagIndexColumn >= 0 && subjectNameIndex >= 0 && timeReminderIndex >= 0) {
                    String dateReminder = cursor.getString(dateReminderIndex);
                    String textReminder = cursor.getString(textReminderIndex);
                    int tagIndex = cursor.getInt(tagIndexColumn);
                    String subjectName = cursor.getString(subjectNameIndex);
                    String timeReminder = cursor.getString(timeReminderIndex);

                    if (firebaseuserboolean) {
                        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

                        checkAndUpdateSubjectNameInFirebase(userId, tagIndex, subjectName);

                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("study_entries").child(userId);
                        String entryId = databaseReference.push().getKey();

                        if (entryId != null) {
                            HashMap<String, Object> reminderData = new HashMap<>();
                            reminderData.put("dateReminder", dateReminder);
                            reminderData.put("textReminder", textReminder);
                            reminderData.put("tagIndex", tagIndex);
                            reminderData.put("subjectName", subjectName);
                            reminderData.put("timeReminder", timeReminder);

                            databaseReference.child(entryId).setValue(reminderData).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    String whereClause = DatabaseHelper.COLUMN_DATE_REMINDER + " = ?";
                                    String[] whereArgs = { dateReminder };
                                    db.delete(DatabaseHelper.TABLE_STUDY, whereClause, whereArgs);
                                    Log.d("StudyActivity", "Reminder entry added to Firebase: " + entryId);
                                } else {
                                    Log.w("StudyActivity", "Failed to add reminder to Firebase", task.getException());
                                }
                            });
                        }
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void loadRemindersFromFirebase() {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("study_entries").child(userId);
        DatabaseReference subjectDatabase = FirebaseDatabase.getInstance().getReference("subjectnames").child(userId);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String todayString = dateFormat.format(new Date()); // dnešný dátum

        database.orderByChild("dateReminder").startAt(todayString).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean hasData = false;
                int i = 0;

                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String entryKey = snapshot.getKey();
                        String dateReminder = snapshot.child("dateReminder").getValue(String.class);
                        String textReminder = snapshot.child("textReminder").getValue(String.class);
                        Integer tagIndex = snapshot.child("tagIndex").getValue(Integer.class);
                        String timeReminder = snapshot.child("timeReminder").getValue(String.class);

                        if (tagIndex == null) continue;

                        final int currentTagIndex = tagIndex;
                        final int currentI = i;

                        subjectDatabase.orderByChild("tagIndex").equalTo(currentTagIndex)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot subjectSnapshot) {
                                        if (subjectSnapshot.exists()) {
                                            for (DataSnapshot subSnap : subjectSnapshot.getChildren()) {
                                                String subjectName = subSnap.child("subjectName").getValue(String.class);
                                                Integer correctTagIndex = subSnap.child("tagIndex").getValue(Integer.class);

                                                if (correctTagIndex != null && !correctTagIndex.equals(currentTagIndex)) {
                                                    database.child(entryKey).child("tagIndex").setValue(correctTagIndex);
                                                }

                                                String newtextReminder = textReminder.length() > 12 ? textReminder.substring(0, 12) : textReminder;
                                                scheduleNotificationOnline(textReminder, dateReminder, timeReminder, subjectName);

                                                if (currentI < 3) {
                                                    switch (currentI) {
                                                        case 0:
                                                            datereminder1.setText(dateReminder);
                                                            timereminder1.setText(timeReminder);
                                                            textreminder1.setText(newtextReminder);
                                                            updateReminderImageView(reminderImageView1, correctTagIndex, subjectName);
                                                            break;
                                                        case 1:
                                                            datereminder2.setText(dateReminder);
                                                            timereminder2.setText(timeReminder);
                                                            textreminder2.setText(newtextReminder);
                                                            updateReminderImageView(reminderImageView2, correctTagIndex, subjectName);
                                                            break;
                                                        case 2:
                                                            datereminder3.setText(dateReminder);
                                                            timereminder3.setText(timeReminder);
                                                            textreminder3.setText(newtextReminder);
                                                            updateReminderImageView(reminderImageView3, correctTagIndex, subjectName);
                                                            break;
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        Log.w("SubjectNameCheck", "Error fetching subject names", databaseError.toException());
                                    }
                                });

                        i++;
                    }
                    hasData = true;
                }

                if (hasData) {
                    setStudyEntryVisibility(View.VISIBLE, i);
                    dots.setVisibility(View.VISIBLE);
                } else {
                    setStudyEntryVisibility(View.INVISIBLE, i);
                    dots.setVisibility(View.INVISIBLE);
                    TextView click = findViewById(R.id.clickhere);
                    click.setVisibility(View.INVISIBLE);
                    noremindersTextView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("StudyActivity", "loadStudyEntries:onCancelled", databaseError.toException());
            }
        });
    }

    private void loadRemindersFromSQL() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        Date today = new Date(); // dnešný dátum
        String selection = DatabaseHelper.COLUMN_DATE_REMINDER + " >= ?";
        String[] selectionArgs = {dateFormat.format(today)};
        String orderBy = DatabaseHelper.COLUMN_DATE_REMINDER + " ASC";

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_STUDY,
                null,
                selection,
                selectionArgs,
                null,
                null,
                orderBy
        );

        boolean hasData = false;
        int i = 0;

        if (cursor.moveToFirst()) {
            hasData = true;
            do {
                int dateReminderIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE_REMINDER);
                int textReminderIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT_REMINDER);
                int tagIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TAG_INDEX);
                int timeReminderIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME_REMINDER);

                String dateReminder = cursor.getString(dateReminderIndex);
                String textReminder = cursor.getString(textReminderIndex);
                String timeReminder = cursor.getString(timeReminderIndex);
                int tagIndexValue = cursor.getInt(tagIndex);

                int correctTagIndex = getCorrectTagIndex(tagIndexValue);
                String subjectName = getSubjectNameFromTagSQL(correctTagIndex);

                // Ak sa tagIndex zmení, aktualizuje sa v DB
                if (tagIndexValue != correctTagIndex) {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.COLUMN_TAG_INDEX, correctTagIndex);
                    db.update(DatabaseHelper.TABLE_STUDY, values, DatabaseHelper.COLUMN_TAG_INDEX + "=?", new String[]{String.valueOf(tagIndexValue)});
                }

                textReminder = textReminder.length() > 12 ? textReminder.substring(0, 12) : textReminder;
                scheduleNotification(textReminder, dateReminder, timeReminder, subjectName);

                final int currentI = i;

                // Zobraziť len maximálne 3 pripomienky
                if (currentI < 3) {
                    switch (currentI) {
                        case 0:
                            datereminder1.setText(dateReminder);
                            timereminder1.setText(timeReminder);
                            textreminder1.setText(textReminder);
                            updateReminderImageView(reminderImageView1, correctTagIndex, subjectName);
                            break;
                        case 1:
                            datereminder2.setText(dateReminder);
                            timereminder2.setText(timeReminder);
                            textreminder2.setText(textReminder);
                            updateReminderImageView(reminderImageView2, correctTagIndex, subjectName);
                            break;
                        case 2:
                            datereminder3.setText(dateReminder);
                            timereminder3.setText(timeReminder);
                            textreminder3.setText(textReminder);
                            updateReminderImageView(reminderImageView3, correctTagIndex, subjectName);
                            break;
                    }
                }
                i++;
            } while (cursor.moveToNext());
        }
        cursor.close();
        setStudyEntryVisibility(View.VISIBLE, i);

        if (!hasData) {
            setStudyEntryVisibility(View.INVISIBLE, i);
            dots.setVisibility(View.INVISIBLE);
            TextView click = findViewById(R.id.clickhere);
            click.setVisibility(View.INVISIBLE);
            noremindersTextView.setVisibility(View.VISIBLE);
        }
    }

    private int getCorrectTagIndex(int currentTagIndex) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_SUBJECT_NAMES,
                new String[]{DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT},
                DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT + "=?",
                new String[]{String.valueOf(currentTagIndex)},
                null, null, null);

        if (cursor.moveToFirst()) {
            int index = cursor.getColumnIndex(DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT);
            int correctTagIndex = cursor.getInt(index);
            cursor.close();
            return correctTagIndex;
        }
        cursor.close();
        return currentTagIndex;  
    }


    private void setStudyEntryVisibility(int visibility, int entryCount) {
        reminderentry1.setVisibility(entryCount > 0 ? visibility : View.GONE);
        reminderentry2.setVisibility(entryCount > 1 ? visibility : View.GONE);
        reminderentry3.setVisibility(entryCount > 2 ? visibility : View.GONE);
    }

    private String getSubjectNameFromTagSQL(int tagIndex) {
        final String[] subjectName = new String[1];

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String[] projection = { DatabaseHelper.COLUMN_SUBJECT_NAME_TAG };
        String selection = DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT + " = ?";
        String[] selectionArgs = { String.valueOf(tagIndex) };

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_SUBJECT_NAMES,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SUBJECT_NAME_TAG);
            if (columnIndex >= 0) {
                subjectName[0] = cursor.getString(columnIndex);
            }
        }
        cursor.close();
        return subjectName[0];
    }

    interface SubjectNameCallback {
        void onSubjectNameLoaded(String subjectName);
    }

    private void getSubjectNameFromTagFirebase(int tagIndex, final SubjectNameCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w("Firebase", "User not authenticated");
            callback.onSubjectNameLoaded("");
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("subjectnames").child(userId);

        ref.orderByChild("tagIndex").equalTo(tagIndex).limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                String subjectName = snapshot.child("subjectName").getValue(String.class);
                                Log.d("Firebase", "Subject Name: " + subjectName);
                                callback.onSubjectNameLoaded(subjectName);
                            }
                        } else {
                            Log.d("Firebase", "No data found for tagIndex: " + tagIndex);
                            callback.onSubjectNameLoaded("");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w("Firebase", "Failed to load subject name", databaseError.toException());
                        callback.onSubjectNameLoaded("");
                    }
                });
    }

    public static String capitalizeWords(String str) {
        StringBuilder result = new StringBuilder();
        for (String word : str.split(" ")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    private void tagAssignment(ImageView tag, EditText subjectEditText, TextView tag_name) {
        if (tag == null) {
            Log.e("TAG_SELECTION", "tag is null");
            return;
        }

        String tagName = getResources().getResourceEntryName(tag.getId());
        this.tagIndex = -1;
        
        if (tagName.startsWith("tag")) {
            try {
                
                this.tagIndex = Integer.parseInt(tagName.substring(3)); 
            } catch (NumberFormatException e) {
                this.tagIndex = -1;
                Log.e("TAG_SELECTION", "Invalid tag format: " + tagName, e);
            }
        }

        if (this.tagIndex == -1) {
            Log.e("TAG_SELECTION", "Tag index is invalid.");
            return;
        }

        Object tagObject = tag.getTag();

        int tagResourceId = (int) tagObject;
        String resourceName = getResources().getResourceEntryName(tagResourceId);

        String colorName = resourceName.replace("tag_", "");
        colorName = capitalizeWords(colorName);

        if (colorName.startsWith("Light")) {
            colorName = "Light " + colorName.substring(5);
        } else if (colorName.startsWith("Dark")) {
            colorName = "Dark " + colorName.substring(4);
        }

        if (tag_name == null) {
            Log.e("TAG_SELECTION", "tag_name TextView is null");
            return;
        }

        tag_name.setText(colorName);
        Log.d("TAG_SELECTION", "Setting tag_name text to: " + colorName);
        
        if (tagIndex != -1) {
            if (!firebaseuserboolean) {
                String subjectName = getSubjectNameFromTagSQL(tagIndex);
                subjectEditText.setText(subjectName);
                tag.setContentDescription(subjectName + " tag");
            } else {
                getSubjectNameFromTagFirebase(tagIndex, subjectName -> {
                    subjectEditText.setText(subjectName);
                    tag.setContentDescription(subjectName + " tag");
                });
            }
        }
    }

    private void reminderbuttondialog() {
        UserActivity.playButtonSound(this);
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_reminder);

        EditText dateEditText = dialog.findViewById(R.id.datereminder);
        EditText textEditText = dialog.findViewById(R.id.textreminder);
        EditText subjectEditText = dialog.findViewById(R.id.subjectwindow);
        EditText timeEditText = dialog.findViewById(R.id.timereminder);

        TextView tag_selection_name = dialog.findViewById(R.id.tag_selection_name);

        if (tagIndex != -1) {
            if (firebaseuserboolean) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Log.w("StudyActivity", "User not signed in.");
                    return;
                }

                String userId = currentUser.getUid();
                DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                        .getReference("subjectnames")
                        .child(userId);

                databaseReference.orderByChild("tagIndex")
                        .equalTo(tagIndex)
                        .limitToFirst(1)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        String subjectName = snapshot.child("subjectName").getValue(String.class);
                                        if (subjectName != null) {
                                            subjectEditText.setText(subjectName);
                                        } else {
                                            Log.w("StudyActivity", "Subject name is null.");
                                        }
                                    }
                                } else {
                                    Log.w("StudyActivity", "No matching subject name found for the given tagIndex in Firebase.");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.w("StudyActivity", "loadSubject:onCancelled", databaseError.toException());
                            }
                        });
            } else {
                SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();
                String query = "SELECT " + DatabaseHelper.COLUMN_SUBJECT_NAME_TAG + " FROM " + DatabaseHelper.TABLE_SUBJECT_NAMES + " WHERE " + DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT + " = ?";
                Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(tagIndex)});

                if (cursor.moveToFirst()) {
                    int subjectNameTagIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SUBJECT_NAME_TAG);
                    if (subjectNameTagIndex >= 0) {
                        String subjectName = cursor.getString(subjectNameTagIndex);
                        subjectEditText.setText(subjectName);
                    } else {
                        Log.w("StudyActivity", "Column 'subject_name_tag' not found in SQLite.");
                    }
                } else {
                    Log.w("StudyActivity", "No matching subject name found for the given tagIndex in SQLite.");
                }

                cursor.close();
                db.close();
            }
        }

        for (int i = 0; i <= 11; i++) {
            int tagId = getResources().getIdentifier("tag" + i, "id", getPackageName());
            ImageView tag = dialog.findViewById(tagId);
            if (tag != null) {
                tag.setTag(tags[i]);
                tag.setOnClickListener(v -> tagAssignment(tag, subjectEditText, tag_selection_name));
            }
        }

        Button saveButton = dialog.findViewById(R.id.save_button);
        Button cancelButton = dialog.findViewById(R.id.cancel_button);

        saveButton.setOnClickListener(v -> {
            UserActivity.playSaveSound(this);
            String textreminder = textEditText.getText().toString().trim();
            String subjectname = subjectEditText.getText().toString().trim();
            String reminderTime = timeEditText.getText().toString().trim();

            if (textreminder.isEmpty()) {
                Toast.makeText(StudyActivity.this, "Please enter a reminder text.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (subjectname.isEmpty()) {
                Toast.makeText(StudyActivity.this, "Please enter a subject name.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (reminderTime.isEmpty()) {
                Toast.makeText(StudyActivity.this, "Please select a time.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (tagIndex == -1) {
                Toast.makeText(StudyActivity.this, "Please select a tag.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedDate == null) {
                Toast.makeText(StudyActivity.this, "Please select a date.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (firebaseuserboolean) {
                if (isInternetAvailable()) {
                    DatabaseReference studyEntriesDatabase = FirebaseDatabase.getInstance().getReference("study_entries");
                    String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                    String entryId = studyEntriesDatabase.child(userId).push().getKey();

                    HashMap<String, Object> entryMap = new HashMap<>();
                    entryMap.put("dateReminder", selectedDate);
                    entryMap.put("timeReminder", reminderTime);
                    entryMap.put("textReminder", textreminder);
                    entryMap.put("subjectName", subjectname);
                    entryMap.put("tagIndex", tagIndex);

                    if (entryId != null) {
                        studyEntriesDatabase.child(userId).child(entryId).setValue(entryMap)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(StudyActivity.this, "Reminder added in Firebase", Toast.LENGTH_SHORT).show();
                                    DatabaseReference subjectNamesDatabase = FirebaseDatabase.getInstance()
                                            .getReference("subjectnames")
                                            .child(userId);

                                    subjectNamesDatabase.orderByChild("tagIndex").equalTo(tagIndex)
                                            .limitToFirst(1)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    if (snapshot.exists()) {
                                                        
                                                        for (DataSnapshot subjectSnap : snapshot.getChildren()) {
                                                            subjectSnap.getRef().child("subjectName").setValue(subjectname);
                                                        }
                                                    } else {
                                                        
                                                        String subjectNameKey = subjectNamesDatabase.push().getKey();
                                                        if (subjectNameKey != null) {
                                                            HashMap<String, Object> subjectMap = new HashMap<>();
                                                            subjectMap.put("subjectName", subjectname);
                                                            subjectMap.put("tagIndex", tagIndex);

                                                            subjectNamesDatabase.child(subjectNameKey).setValue(subjectMap)
                                                                    .addOnFailureListener(e ->
                                                                            Toast.makeText(StudyActivity.this, "Failed to save subject name", Toast.LENGTH_SHORT).show());
                                                        }
                                                    }
                                                    dialog.dismiss();
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    Toast.makeText(StudyActivity.this, "Failed to check subject name: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(StudyActivity.this, "Failed to add reminder", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    saveToSqlDatabase(selectedDate, reminderTime, textreminder, subjectname, tagIndex, dialog);
                    Toast.makeText(StudyActivity.this, "No internet connection. Saved in SQL database.", Toast.LENGTH_SHORT).show();
                }
            } else {
                saveToSqlDatabase(selectedDate, reminderTime, textreminder, subjectname, tagIndex, dialog);
            }
        });

        dateEditText.setFocusableInTouchMode(true);
        dateEditText.setFocusableInTouchMode(true);
        dateEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                UserActivity.playButtonSound(this);
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(StudyActivity.this,
                        (view, year1, monthOfYear, dayOfMonth1) -> {
                            selectedDate = String.format("%02d.%02d.%d", dayOfMonth1, monthOfYear + 1, year1);
                            dateEditText.setText(selectedDate);
                        }, year, month, dayOfMonth);

                datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
                datePickerDialog.show();
            }
            return true; 
        });

        timeEditText.setFocusableInTouchMode(true);
        timeEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                UserActivity.playButtonSound(this);
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(StudyActivity.this,
                        (view, hourOfDay, minute1) -> {
                            reminderTime = String.format("%02d:%02d", hourOfDay, minute1);
                            timeEditText.setText(reminderTime);
                        }, hour, minute, true);

                timePickerDialog.show();
            }
            return true; 
        });


        cancelButton.setOnClickListener(v -> {
            UserActivity.playCancelSound(this);
            dialog.dismiss();
        });
        dialog.show();
    }
    private void saveToSqlDatabase(String date, String time, String text, String subject, int tagIndex, Dialog dialog) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DATE_REMINDER, date);
        values.put(DatabaseHelper.COLUMN_TIME_REMINDER, time);
        values.put(DatabaseHelper.COLUMN_TEXT_REMINDER, text);
        values.put(DatabaseHelper.COLUMN_SUBJECT_NAME, subject);
        values.put(DatabaseHelper.COLUMN_TAG_INDEX, tagIndex);

        long newRowId = db.insert(DatabaseHelper.TABLE_STUDY, null, values);
        if (newRowId != -1) {
            Toast.makeText(StudyActivity.this, "Reminder added in SQL database", Toast.LENGTH_SHORT).show();
            recreate();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            Toast.makeText(StudyActivity.this, "Error saving reminder in SQL database", Toast.LENGTH_SHORT).show();
        }

        ContentValues subjectValues = new ContentValues();
        subjectValues.put(DatabaseHelper.COLUMN_SUBJECT_NAME_TAG, subject);
        subjectValues.put(DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT, tagIndex);

        long newSubjectRowId = db.insert(DatabaseHelper.TABLE_SUBJECT_NAMES, null, subjectValues);
        if (newSubjectRowId == -1) {
            Toast.makeText(StudyActivity.this, "Error saving subject to table_subject_names", Toast.LENGTH_SHORT).show();
        }

        dialog.dismiss();
    }




    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }
    private void setupReminderEntryListeners() {
        reminderentry1.setOnClickListener(v -> loadFullReminderDetails(datereminder1.getText().toString(), timereminder1.getText().toString()));
        reminderentry2.setOnClickListener(v -> loadFullReminderDetails(datereminder2.getText().toString(), timereminder2.getText().toString()));
        reminderentry3.setOnClickListener(v -> loadFullReminderDetails(datereminder3.getText().toString(), timereminder3.getText().toString()));

        reminderentry1.setOnLongClickListener(v -> {
            UserActivity.playCancelSound(this);
            showDeleteConfirmationDialog(0);
            return true;
        });

        reminderentry2.setOnLongClickListener(v -> {
            UserActivity.playCancelSound(this);
            showDeleteConfirmationDialog(1);
            return true;
        });

        reminderentry3.setOnLongClickListener(v -> {
            UserActivity.playCancelSound(this);
            showDeleteConfirmationDialog(2);
            return true;
        });
    }
    private void showDeleteConfirmationDialog(int reminderindex) {
        new AlertDialog.Builder(StudyActivity.this)
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete this reminder?")
                .setPositiveButton("Yes", (dialog, which) -> confirmAndDeleteReminder(reminderindex))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void confirmAndDeleteReminder(int reminderIndex) {
        if(reminderIndex == 0){
            fetchReminderDetailsAndDelete(datereminder1, timereminder1, firebaseuserboolean);
        } else if (reminderIndex == 1) {
            fetchReminderDetailsAndDelete(datereminder2, timereminder2, firebaseuserboolean);
        } else if (reminderIndex == 2) {
            fetchReminderDetailsAndDelete(datereminder3,timereminder3, firebaseuserboolean);
        }

    }

    private void fetchReminderDetailsAndDelete(TextView date, TextView time, boolean isFirebase) {
        String dateReminder = (String) date.getText();
        String timeReminder = (String) time.getText();

        if (isFirebase) {
            String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            DatabaseReference database = FirebaseDatabase.getInstance().getReference("study_entries").child(userId);

            
            database.orderByChild("dateReminder").equalTo(dateReminder) 
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    String fetchedTime = snapshot.child("timeReminder").getValue(String.class);
                                    if (fetchedTime != null && fetchedTime.equals(timeReminder)) {
                                        snapshot.getRef().removeValue()
                                                .addOnSuccessListener(aVoid -> Toast.makeText(StudyActivity.this, "Reminder deleted", Toast.LENGTH_SHORT).show())
                                                .addOnFailureListener(e -> Toast.makeText(StudyActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show());
                                        return;
                                    }
                                }
                                Toast.makeText(StudyActivity.this, "Reminder not found", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.w("Firebase", "Failed to delete reminder", databaseError.toException());
                        }
                    });
        } else {
            DatabaseHelper databaseHelper = new DatabaseHelper(StudyActivity.this);
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            int deletedRows = db.delete(DatabaseHelper.TABLE_STUDY,
                    DatabaseHelper.COLUMN_DATE_REMINDER + " = ? AND " + DatabaseHelper.COLUMN_TIME_REMINDER + " = ?",
                    new String[]{dateReminder, timeReminder});

            if (deletedRows > 0) {
                Toast.makeText(StudyActivity.this, "Reminder deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(StudyActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
            }
            db.close();
        }
    }

    public void loadFullReminderDetails(String date, String time) {
        if (firebaseuserboolean) {
            loadReminderDetailsFromFirebase(date,time);
        } else {
            loadReminderDetailsFromSQL(date,time);
        }
    }

    private void loadReminderDetailsFromFirebase(String dateReminder, String timeReminder) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("study_entries").child(userId);

        database.orderByChild("dateReminder")
                .equalTo(dateReminder)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                String date = snapshot.child("dateReminder").getValue(String.class);
                                String text = snapshot.child("textReminder").getValue(String.class);
                                String time = snapshot.child("timeReminder").getValue(String.class);
                                String subject = snapshot.child("subjectName").getValue(String.class);
                                Long tagIndexLong = snapshot.child("tagIndex").getValue(Long.class);
                                int tagIndex = (tagIndexLong != null) ? tagIndexLong.intValue() : -1; 

                                if (timeReminder.equals(time)) {
                                    openEditReminderDialog(date, time, text, subject, tagIndex);
                                }
                            }
                        } else {
                            Toast.makeText(StudyActivity.this, "No reminder found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w("Firebase", "Failed to load reminder details", databaseError.toException());
                    }
                });
    }

    private void loadReminderDetailsFromSQL(String dateReminder, String timeReminder) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String selection = DatabaseHelper.COLUMN_DATE_REMINDER + " = ? AND " + DatabaseHelper.COLUMN_TIME_REMINDER + " = ?";
        String[] selectionArgs = {dateReminder, timeReminder};

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_STUDY,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            int dateReminderIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE_REMINDER);
            int textReminderIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT_REMINDER);
            int timeReminderIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME_REMINDER);
            int subjectNameIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SUBJECT_NAME);
            int tagIndexColumn = cursor.getColumnIndex(DatabaseHelper.COLUMN_TAG_INDEX);

            String dateReminderFromDB = dateReminderIndex != -1 ? cursor.getString(dateReminderIndex) : "";
            String textReminderFromDB = textReminderIndex != -1 ? cursor.getString(textReminderIndex) : "";
            String timeReminderFromDB = timeReminderIndex != -1 ? cursor.getString(timeReminderIndex) : "";
            String subjectNameFromDB = subjectNameIndex != -1 ? cursor.getString(subjectNameIndex) : "";
            int tagIndexFromDB = (tagIndexColumn != -1) ? cursor.getInt(tagIndexColumn) : -1;

            openEditReminderDialog(dateReminderFromDB, timeReminderFromDB, textReminderFromDB, subjectNameFromDB, tagIndexFromDB);
        } else {
            Toast.makeText(this, "No reminder found", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
    }

    private int selectedTagIndex = -1;

    public void setTagSelectionBySubject(Dialog dialog, int tagIndex, String subjectName, TextView tag_selection_name, EditText subjectEditText) {
        if (tagIndex != -1) {
            int tagResourceId = tags[tagIndex];
            String resourceName = getResources().getResourceEntryName(tagResourceId);
            String colorName = resourceName.replace("tag_", "");
            colorName = capitalizeWords(colorName);
            if (colorName.startsWith("Light")) {
                colorName = "Light " + colorName.substring(5);
            } else if (colorName.startsWith("Dark")) {
                colorName = "Dark " + colorName.substring(4);
            }
            
            tag_selection_name.setText(colorName);

            subjectEditText.setText(subjectName);
        }
    }

    private void openEditReminderDialog(String date, String time, String text, String subject, int tagIndex) {
        UserActivity.playButtonSound(this);
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_reminder);

        EditText dateEditText = dialog.findViewById(R.id.datereminder);
        EditText textEditText = dialog.findViewById(R.id.textreminder);
        EditText subjectEditText = dialog.findViewById(R.id.subjectwindow);
        EditText timeEditText = dialog.findViewById(R.id.timereminder);

        TextView tag_selection_name = dialog.findViewById(R.id.tag_selection_name);

        dateEditText.setText(date);
        textEditText.setText(text);
        subjectEditText.setText(subject);
        timeEditText.setText(time);

        if (tagIndex != -1) {
            setTagSelectionBySubject(dialog, tagIndex, subject, tag_selection_name, subjectEditText);
        }

        if (tagIndex != -1) {
            if (firebaseuserboolean) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Log.w("StudyActivity", "User not signed in.");
                    return;
                }

                String userId = currentUser.getUid();
                DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                        .getReference("subjectnames")
                        .child(userId);

                databaseReference.orderByChild("tagIndex")
                        .equalTo(tagIndex)
                        .limitToFirst(1)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        String subjectName = snapshot.child("subjectName").getValue(String.class);
                                        if (subjectName != null) {
                                            subjectEditText.setText(subjectName);
                                        } else {
                                            Log.w("StudyActivity", "subjectName is null in snapshot.");
                                        }
                                    }
                                } else {
                                    Log.w("StudyActivity", "No matching subject found in Firebase for the given tagIndex.");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.w("StudyActivity", "loadSubject:onCancelled", databaseError.toException());
                            }
                        });
        } else {
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                String[] projection = { DatabaseHelper.COLUMN_SUBJECT_NAME_TAG };
                String selection = DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT + " = ?";
                String[] selectionArgs = { String.valueOf(tagIndex) };

                Cursor cursor = db.query(DatabaseHelper.TABLE_SUBJECT_NAMES, projection, selection, selectionArgs, null, null, null);

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int subjectNameIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SUBJECT_NAME_TAG);

                        if (subjectNameIndex != -1) {
                            String subjectName = cursor.getString(subjectNameIndex);
                            if (subjectName != null) {
                                subjectEditText.setText(subjectName);
                            }
                        } else {
                            Log.w("StudyActivity", DatabaseHelper.COLUMN_SUBJECT_NAME_TAG + " column not found in the SQLite database.");
                        }
                    }
                    cursor.close();
                } else {
                    Log.w("StudyActivity", "No matching subject found in SQLite for the given tagIndex.");
                }
            }
        }


        for (int i = 0; i <= 11; i++) {
            final int selecttagIndex = i;
            int tagId = getResources().getIdentifier("tag" + i, "id", getPackageName());
            ImageView tag = dialog.findViewById(tagId);
            if (tag != null) {
                tag.setTag(tags[i]);
                tag.setOnClickListener(v -> {
                    selectedTagIndex = selecttagIndex;  
                    tagAssignment(tag, subjectEditText, tag_selection_name);
                });
            }
        }


        Button saveButton = dialog.findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            UserActivity.playSaveSound(this);
            String updatedDate = dateEditText.getText().toString().trim();
            String updatedText = textEditText.getText().toString().trim();
            String updatedTime = timeEditText.getText().toString().trim();
            String updatedSubject = subjectEditText.getText().toString().trim();

            if (updatedDate.isEmpty()) {
                Toast.makeText(StudyActivity.this, "Please select a date.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (updatedText.isEmpty()) {
                Toast.makeText(StudyActivity.this, "Please enter a reminder text.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (updatedTime.isEmpty()) {
                Toast.makeText(StudyActivity.this, "Please select a time.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (updatedSubject.isEmpty()) {
                Toast.makeText(StudyActivity.this, "Please enter a subject.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedTagIndex != -1 && selectedTagIndex != tagIndex) {
                
                if (firebaseuserboolean) {
                    updateFirebaseReminder(date, time, updatedDate, updatedTime, updatedText, updatedSubject, selectedTagIndex);
                } else {
                    updateSQLReminder(date, time, updatedDate, updatedTime, updatedText, updatedSubject, selectedTagIndex);
                }
            } else {
                
                if (firebaseuserboolean) {
                    updateFirebaseReminder(date, time, updatedDate, updatedTime, updatedText, updatedSubject, tagIndex);
                } else {
                    updateSQLReminder(date, time, updatedDate, updatedTime, updatedText, updatedSubject, tagIndex);
                }
            }

            dialog.dismiss();
        });

        dateEditText.setFocusableInTouchMode(true);
        dateEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                UserActivity.playButtonSound(this);
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(StudyActivity.this,
                        (view, year1, monthOfYear, dayOfMonth1) -> {
                            selectedDate = String.format("%02d.%02d.%d", dayOfMonth1, monthOfYear + 1, year1);
                            dateEditText.setText(selectedDate);
                        }, year, month, dayOfMonth);

                datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
                datePickerDialog.show();
            }
            return true; 
        });

        timeEditText.setFocusableInTouchMode(true);
        timeEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                UserActivity.playButtonSound(this);
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(StudyActivity.this,
                        (view, hourOfDay, minute1) -> {
                            reminderTime = String.format("%02d:%02d", hourOfDay, minute1);
                            timeEditText.setText(reminderTime);
                        }, hour, minute, true);

                timePickerDialog.show();
            }
            return true; 
        });


        Button cancelButton = dialog.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            UserActivity.playCancelSound(this);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateFirebaseReminder(String oldDate, String oldTime, String newDate, String newTime, String newText, String newSubject, int newTagIndex) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("study_entries").child(userId);

        database.orderByChild("dateReminder")
                .equalTo(oldDate)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean reminderFound = false;

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String dbTimeReminder = snapshot.child("timeReminder").getValue(String.class);

                            if (oldTime.equals(dbTimeReminder)) {
                                reminderFound = true;

                                snapshot.getRef().child("dateReminder").setValue(newDate);
                                snapshot.getRef().child("timeReminder").setValue(newTime);
                                snapshot.getRef().child("textReminder").setValue(newText);
                                snapshot.getRef().child("subjectName").setValue(newSubject);
                                snapshot.getRef().child("tagIndex").setValue(newTagIndex);

                                
                                DatabaseReference subjectRef = FirebaseDatabase.getInstance()
                                        .getReference("subjectnames")
                                        .child(userId);

                                subjectRef.orderByChild("tagIndex").equalTo(newTagIndex)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot subjectSnapshot) {
                                                if (subjectSnapshot.exists()) {
                                                    for (DataSnapshot subSnap : subjectSnapshot.getChildren()) {
                                                        subSnap.getRef().child("subjectName").setValue(newSubject);
                                                    }
                                                } else {
                                                    String newKey = subjectRef.push().getKey();
                                                    if (newKey != null) {
                                                        HashMap<String, Object> subjectData = new HashMap<>();
                                                        subjectData.put("subjectName", newSubject);
                                                        subjectData.put("tagIndex", newTagIndex);
                                                        subjectRef.child(newKey).setValue(subjectData);
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.w("Firebase", "Failed to update subject name", error.toException());
                                            }
                                        });

                                Toast.makeText(StudyActivity.this, "Reminder updated in Firebase", Toast.LENGTH_SHORT).show();
                                loadRemindersFromFirebase();
                                break;
                            }
                        }

                        if (!reminderFound) {
                            Toast.makeText(StudyActivity.this, "No reminder found for the given date and time", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w("Firebase", "Failed to update reminder", databaseError.toException());
                    }
                });
    }

    private void updateSQLReminder(String oldDate, String oldTime, String newDate, String newTime, String newText, String newSubject, int newTagIndex) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        String selection = DatabaseHelper.COLUMN_DATE_REMINDER + " = ? AND " + DatabaseHelper.COLUMN_TIME_REMINDER + " = ?";
        String[] selectionArgs = {oldDate, oldTime};

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DATE_REMINDER, newDate);
        values.put(DatabaseHelper.COLUMN_TIME_REMINDER, newTime);
        values.put(DatabaseHelper.COLUMN_TEXT_REMINDER, newText);
        values.put(DatabaseHelper.COLUMN_SUBJECT_NAME, newSubject);
        values.put(DatabaseHelper.COLUMN_TAG_INDEX, newTagIndex);

        int rowsUpdated = db.update(DatabaseHelper.TABLE_STUDY, values, selection, selectionArgs);

        if (rowsUpdated > 0) {
            
            String subjectQuery = "SELECT * FROM " + DatabaseHelper.TABLE_SUBJECT_NAMES + " WHERE " + DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT + " = ?";
            Cursor cursor = db.rawQuery(subjectQuery, new String[]{String.valueOf(newTagIndex)});

            ContentValues subjectValues = new ContentValues();
            subjectValues.put(DatabaseHelper.COLUMN_SUBJECT_NAME_TAG, newSubject);
            subjectValues.put(DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT, newTagIndex);

            if (cursor.moveToFirst()) {
                
                db.update(DatabaseHelper.TABLE_SUBJECT_NAMES, subjectValues,
                        DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT + " = ?",
                        new String[]{String.valueOf(newTagIndex)});
            } else {
                
                db.insert(DatabaseHelper.TABLE_SUBJECT_NAMES, null, subjectValues);
            }

            cursor.close();

            Toast.makeText(this, "Reminder updated in SQL database", Toast.LENGTH_SHORT).show();
            loadRemindersFromSQL();
        } else {
            Toast.makeText(this, "Failed to update reminder in SQL database", Toast.LENGTH_SHORT).show();
        }
    }

    private void subjectchangebuttondialog() {
        UserActivity.playButtonSound(this);
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_changesubject);

        EditText subjectEditText = dialog.findViewById(R.id.subjectwindow);
        TextView tag_selection_name = dialog.findViewById(R.id.tag_selection_name);

        if (tagIndex != -1) {
            if (firebaseuserboolean) {
                DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                        .getReference("subjectnames")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

                databaseReference.orderByChild("tagIndex")
                        .equalTo(tagIndex)
                        .limitToFirst(1)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        String subjectName = snapshot.child("subjectName").getValue(String.class);
                                        if (subjectName != null) {
                                            subjectEditText.setText(subjectName);
                                        }
                                    }
                                } else {
                                    Log.w("StudyActivity", "No matching subject name found for the given tagIndex.");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.w("StudyActivity", "loadSubject:onCancelled", databaseError.toException());
                            }
                        });

            } else {
                SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();
                String query = "SELECT " + DatabaseHelper.COLUMN_SUBJECT_NAME_TAG + " FROM " + DatabaseHelper.TABLE_SUBJECT_NAMES + " WHERE " + DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT + " = ?";
                Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(tagIndex)});

                if (cursor.moveToFirst()) {
                    int subjectNameTagIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SUBJECT_NAME_TAG);
                    if (subjectNameTagIndex >= 0) {
                        String subjectName = cursor.getString(subjectNameTagIndex);
                        subjectEditText.setText(subjectName);
                    } else {
                        Log.w("StudyActivity", "Column 'subject_name_tag' not found in SQLite.");
                    }
                } else {
                    Log.w("StudyActivity", "No matching subject name found for the given tagIndex in SQLite.");
                }

                cursor.close();
                db.close();
            }
        }

        for (int i = 0; i <= 11; i++) {
            int tagId = getResources().getIdentifier("tag" + i, "id", getPackageName());
            ImageView tag = dialog.findViewById(tagId);
            if (tag != null) {
                tag.setTag(tags[i]);
                tag.setOnClickListener(v -> tagAssignment(tag, subjectEditText, tag_selection_name));
                Log.d("TAG", "subjectchangebuttondialog: " );
            }
        }

        Button saveButton = dialog.findViewById(R.id.save_button);
        Button cancelButton = dialog.findViewById(R.id.cancel_button);

        saveButton.setOnClickListener(v -> {
            UserActivity.playSaveSound(this);
            String subjectname = subjectEditText.getText().toString().trim();

            if (subjectname.isEmpty()) {
                Toast.makeText(StudyActivity.this, "Please enter a subject name.", Toast.LENGTH_SHORT).show();
                return;  
            }

            if (tagIndex == -1) {
                Toast.makeText(StudyActivity.this, "Please select a tag.", Toast.LENGTH_SHORT).show();
                return;  
            }

            SharedPreferences sharedPreferences = getSharedPreferences("study_preferences", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("subjectName", subjectname);
            editor.putInt("tagIndex", tagIndex);
            editor.apply();

            if (firebaseuserboolean) {
                DatabaseReference baseRef = FirebaseDatabase.getInstance().getReference("subjectnames").child(userId);

                baseRef.orderByChild("tagIndex").equalTo(tagIndex).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot subjectSnap : snapshot.getChildren()) {
                                subjectSnap.getRef().child("subjectName").setValue(subjectname);
                            }
                            Toast.makeText(StudyActivity.this, "Subject updated in Firebase", Toast.LENGTH_SHORT).show();
                        } else {
                            String key = baseRef.push().getKey();
                            if (key != null) {
                                Map<String, Object> subjectData = new HashMap<>();
                                subjectData.put("subjectName", subjectname);
                                subjectData.put("tagIndex", tagIndex);
                                baseRef.child(key).setValue(subjectData)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(StudyActivity.this, "Subject added in Firebase", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(StudyActivity.this, "Error saving subject", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }

                        subjectstudywindow.setText(subjectname);
                        saveTagIndex(tagIndex);
                        dialog.dismiss();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(StudyActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        } else {
                SQLiteDatabase db = new DatabaseHelper(this).getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_SUBJECT_NAME_TAG, subjectname);
                values.put(DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT, tagIndex);

                int rowsUpdated = db.update(DatabaseHelper.TABLE_SUBJECT_NAMES, values,
                        DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT + " = ?", new String[]{String.valueOf(tagIndex)});

                if (rowsUpdated <= 0){
                    values.put(DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT, tagIndex);
                    db.insert(DatabaseHelper.TABLE_SUBJECT_NAMES, null, values);
                  }

                db.close();
            }
            subjectstudywindow.setText(subjectname);

            saveTagIndex(tagIndex);

            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            UserActivity.playCancelSound(this);
            dialog.dismiss();
        });

        dialog.show();
    }



    private void saveTagIndex(int tagIndex) {
        SharedPreferences sharedPreferences = getSharedPreferences("StudyPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("tagIndex", tagIndex);
        editor.apply();
    }

    public void loadStudySessionsFromSQL() {
        studyList = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        HashMap<String, Float> dataMap = new HashMap<>();
        HashMap<String, Integer> subjectColorMap = new HashMap<>();
        HashMap<String, Float> goalMap = new HashMap<>();

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_STUDY_SESSION,
                null,
                null,
                null,
                null,
                null,
                DatabaseHelper.COLUMN_DATE + " DESC, " + DatabaseHelper.COLUMN_TIME + " DESC"
        );
        if (cursor != null && cursor.moveToFirst()) {
            int dateColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE);
            int subjectNameColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SUBJECT_NAME_SESSION);
            int textColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT);
            int tagIndexColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TAG_INDEX_SESSION);
            int stopwatchTimeColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_STOPWATCH_TIME);
            int loadTimeColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME);

            if (dateColumnIndex >= 0 && subjectNameColumnIndex >= 0 && textColumnIndex >= 0
                    && tagIndexColumnIndex >= 0 && stopwatchTimeColumnIndex >= 0 && loadTimeColumnIndex >= 0) {
                do {
                    String date = cursor.getString(dateColumnIndex);
                    String subjectName = cursor.getString(subjectNameColumnIndex);
                    String text = cursor.getString(textColumnIndex);
                    int tagIndex = cursor.getInt(tagIndexColumnIndex);
                    String stopwatchTime = cursor.getString(stopwatchTimeColumnIndex);
                    String loadTime = cursor.getString(loadTimeColumnIndex);

                    if (dateList == null) {
                        dateList = new ArrayList<>();
                    }

                    if (!dateList.contains(date)) {
                        dateList.add(date);
                    }

                    studyList.add(new String[]{date, subjectName, text, String.valueOf(tagIndex), stopwatchTime, loadTime});

                    if (subjectName != null && stopwatchTime != null) {

                        float timeInSeconds = parseTimeToSeconds(stopwatchTime);


                        float timeInMinutes = timeInSeconds / 60f;
                        if (subjectName.length() > 9) {
                            subjectName = subjectName.substring(0, 6) + "...";
                        }


                        dataMap.put(subjectName, dataMap.getOrDefault(subjectName, 0f) + timeInMinutes);


                        if (!subjectColorMap.containsKey(subjectName)) {
                            int color = tagIndex < tagindexcolors.length ? tagindexcolors[tagIndex] : Color.GRAY;
                            subjectColorMap.put(subjectName, color);
                        }
                    }
                    nostudyTextView.setVisibility(View.INVISIBLE);


                } while (cursor.moveToNext());
            } else {
                Log.e("AllStudySessionsActivity", "Column index is invalid");
            }
        } else {
            nostudyTextView.setVisibility(View.VISIBLE);
        }

        if (cursor != null) {
            cursor.close();
        }

        if (!studyList.isEmpty()) {
            updateStudyChart(dataMap, goalMap, subjectColorMap);
        } else {
            Log.e("StudyActivity", "No study sessions available, skipping chart update.");
            MainActivity.displayNoDataMessage(progressionstudyTable);
        }

        calculateStreak();
        updateRecyclerView();
    }

    public void loadStudySessionsFromFirebase() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("study_sessions").child(userId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                studyList = new ArrayList<>();
                HashMap<String, Float> dataMap = new HashMap<>();
                HashMap<String, Integer> subjectColorMap = new HashMap<>();
                HashMap<String, Float> goalMap = new HashMap<>();

                if (dateList == null) {
                    dateList = new ArrayList<>();
                } else {
                    dateList.clear();
                }

                if (dataSnapshot.exists()) {
                    List<DataSnapshot> snapshotList = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        snapshotList.add(snapshot);
                    }
                    Collections.reverse(snapshotList);

                    for (DataSnapshot snapshot : snapshotList) {
                        String date = snapshot.child("date").getValue(String.class);
                        String subjectName = snapshot.child("subject_name").getValue(String.class);
                        String text = snapshot.child("text").getValue(String.class);
                        Integer tagIndexObj = snapshot.child("tag_index").getValue(Integer.class);
                        int tagIndex = (tagIndexObj != null) ? tagIndexObj : 0;
                        String stopwatchTime = snapshot.child("stopwatch_time").getValue(String.class);
                        String loadTime = snapshot.child("time").getValue(String.class);

                        Log.d("FirebaseData", "Date: " + date + ", Subject: " + subjectName +
                                ", Text: " + text + ", Tag Index: " + tagIndex +
                                ", Stopwatch Time: " + stopwatchTime + ", Load Time: " + loadTime);

                        if (!dateList.contains(date)) {
                            dateList.add(date);
                        }

                        studyList.add(new String[]{date, subjectName, text, String.valueOf(tagIndex), stopwatchTime, loadTime});

                        if (subjectName != null && stopwatchTime != null) {
                            float timeInSeconds = parseTimeToSeconds(stopwatchTime);
                            float timeInMinutes = timeInSeconds / 60f;
                            if (subjectName.length() > 9) {
                                subjectName = subjectName.substring(0, 6) + "...";
                            }
                            dataMap.put(subjectName, dataMap.getOrDefault(subjectName, 0f) + timeInMinutes);

                            if (!subjectColorMap.containsKey(subjectName)) {
                                int color = tagIndex < tagindexcolors.length ? tagindexcolors[tagIndex] : Color.GRAY;
                                subjectColorMap.put(subjectName, color);
                            }
                        }
                    }
                    if (!studyList.isEmpty()) {
                        updateStudyChart(dataMap, goalMap, subjectColorMap);
                    } else {
                        Log.e("StudyActivity", "No study sessions available, skipping chart update.");
                        MainActivity.displayNoDataMessage(progressionstudyTable);
                    }

                    nostudyTextView.setVisibility(View.INVISIBLE);
                } else {
                    Log.w("FirebaseData", "No data found for userId: " + userId);
                    loadStudySessionsFromSQL();
                }
                calculateStreak();
                updateRecyclerView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("AllStudySessionsActivity", "loadStudySessions:onCancelled", databaseError.toException());
            }
        });
    }

    private void updateRecyclerView() {
        if (studyAdapter == null) {
            studyAdapter = new StudyAdapter(studyList, tags);
            studyRecyclerView.setAdapter(studyAdapter);
        } else {
            studyAdapter.notifyDataSetChanged();
        }

    }

    private final int[] tagindexcolors = {
            Color.parseColor("#d64358"),
            Color.parseColor("#d88950"),
            Color.parseColor("#f9e682"),
            Color.parseColor("#c2f982"),
            Color.parseColor("#82f9f6"),
            Color.parseColor("#9082f9"),
            Color.parseColor("#71172b"),
            Color.parseColor("#a15d0c"),
            Color.parseColor("#c8b432"),
            Color.parseColor("#54901a"),
            Color.parseColor("#1a6a90"),
            Color.parseColor("#481a90")
    };

    private void getDataProgressSubjects() {
        List<BarEntry> barArrayList = new ArrayList<>();
        List<String> subjectLabels = new ArrayList<>();
        List<Integer> barColors = new ArrayList<>();

        HashMap<String, Float> dataMap = new HashMap<>();
        HashMap<String, Integer> subjectColorMap = new HashMap<>();

        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);

        if (firebaseuserboolean) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("study_sessions").child(userId);

                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String subjectName = snapshot.child("subject_name").getValue(String.class);
                            String stopwatchTime = snapshot.child("stopwatch_time").getValue(String.class);
                            int tagIndex = snapshot.child("tag_index").getValue(Integer.class);
                            String dateString = snapshot.child("date").getValue(String.class);

                            if (dateString != null) {
                                SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.y", Locale.getDefault());
                                try {
                                    Date date = dateFormat.parse(dateString);
                                    Calendar sessionCal = Calendar.getInstance();
                                    sessionCal.setTime(date);

                                    int sessionMonth = sessionCal.get(Calendar.MONTH);
                                    int sessionYear = sessionCal.get(Calendar.YEAR);

                                    if (sessionMonth != currentMonth || sessionYear != currentYear) {
                                        continue;
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    continue;
                                }
                            }

                            if (subjectName != null && stopwatchTime != null) {
                                float timeInSeconds = parseTimeToSeconds(stopwatchTime);

                                if (subjectName.length() > 9) {
                                    subjectName = subjectName.substring(0, 6) + "...";
                                }
                                if (dataMap.containsKey(subjectName)) {
                                    dataMap.put(subjectName, dataMap.get(subjectName) + timeInSeconds);
                                } else {
                                    dataMap.put(subjectName, timeInSeconds);
                                }

                                if (!subjectColorMap.containsKey(subjectName)) {
                                    int color = tagIndex < tagindexcolors.length ? tagindexcolors[tagIndex] : Color.GRAY;
                                    subjectColorMap.put(subjectName, color);
                                }
                            }
                        }
                        displayChartData(dataMap, subjectColorMap, subjectLabels, barArrayList, barColors);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                        Log.w("Firebase", "Failed to read value.", databaseError.toException());
                    }
                });
            }
        } else {
            String query = "SELECT subject_name, stopwatch_time, tag_index, date FROM StudySession";
            Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(query, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int subjectNameColumnIndex = cursor.getColumnIndex("subject_name");
                    int stopwatchTimeColumnIndex = cursor.getColumnIndex("stopwatch_time");
                    int tagIndexColumnIndex = cursor.getColumnIndex("tag_index");
                    int dateIndex = cursor.getColumnIndex("date");

                    if (subjectNameColumnIndex != -1 && stopwatchTimeColumnIndex != -1 && tagIndexColumnIndex != -1 && dateIndex != -1) {
                        String subjectName = cursor.getString(subjectNameColumnIndex);
                        String stopwatchTime = cursor.getString(stopwatchTimeColumnIndex);
                        int tagIndex = cursor.getInt(tagIndexColumnIndex);
                        String dateStr = cursor.getString(dateIndex); // date in d.M.y

                        if (subjectName.length() > 9) {
                            subjectName = subjectName.substring(0, 6) + "...";
                        }
                        if (dateStr != null) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.y", Locale.getDefault());
                            try {
                                Date date = dateFormat.parse(dateStr);
                                Calendar sessionCal = Calendar.getInstance();
                                sessionCal.setTime(date);

                                int sessionMonth = sessionCal.get(Calendar.MONTH);
                                int sessionYear = sessionCal.get(Calendar.YEAR);

                                if (sessionMonth != currentMonth || sessionYear != currentYear) {
                                    continue;
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                                continue;
                            }
                        }
                        float timeInSeconds = parseTimeToSeconds(stopwatchTime);

                        if (dataMap.containsKey(subjectName)) {
                            dataMap.put(subjectName, dataMap.get(subjectName) + timeInSeconds);
                        } else {
                            dataMap.put(subjectName, timeInSeconds);
                        }

                        if (!subjectColorMap.containsKey(subjectName)) {
                            int color = tagIndex < tagindexcolors.length ? tagindexcolors[tagIndex] : Color.GRAY;
                            subjectColorMap.put(subjectName, color);
                        }
                    }
                }
                cursor.close();
            }

            displayChartData(dataMap, subjectColorMap, subjectLabels, barArrayList, barColors);
        }
    }

    private void displayChartData(HashMap<String, Float> dataMap, HashMap<String, Integer> subjectColorMap,
                                  List<String> subjectLabels, List<BarEntry> barArrayList, List<Integer> barColors) {
        boolean hasData = false;
        float maxTime = 0f;

        for (String subject : dataMap.keySet()) {
            float timeInSeconds = dataMap.get(subject);
            maxTime = Math.max(maxTime, timeInSeconds);

            subjectLabels.add(subject);
            barArrayList.add(new BarEntry(subjectLabels.size() - 1, timeInSeconds));
            barColors.add(subjectColorMap.get(subject));
            hasData = true;
        }

        if (!hasData) {
            findViewById(R.id.progressionsubjectsTextView).setVisibility(View.GONE);
            findViewById(R.id.progressionsubjectsTable).setVisibility(View.GONE);
            findViewById(R.id.progressionstudyTextView).setVisibility(View.GONE);
            findViewById(R.id.progressionstudyTable).setVisibility(View.GONE);

            TextView progressionStudyTextView = findViewById(R.id.progressionstudyTextView);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) progressionStudyTextView.getLayoutParams();
            params.topToBottom = R.id.dots;
            progressionStudyTextView.setLayoutParams(params);
            MainActivity.displayNoDataMessage(barChart);
            return;
        } else {
            findViewById(R.id.progressionsubjectsTextView).setVisibility(View.VISIBLE);
            findViewById(R.id.progressionsubjectsTable).setVisibility(View.VISIBLE);
            findViewById(R.id.progressionstudyTextView).setVisibility(View.VISIBLE);
            TextView progressionStudyTextView = findViewById(R.id.progressionstudyTextView);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) progressionStudyTextView.getLayoutParams();
            params.topToBottom = R.id.progressionsubjectsTable;
            progressionStudyTextView.setLayoutParams(params);
        }

        BarDataSet barDataSet = new BarDataSet(barArrayList, "Stopwatch Time by Subject");
        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);

        barDataSet.setColors(barColors);
        barDataSet.setValueTextColor(Color.BLACK);
        barDataSet.setDrawValues(true);
        barDataSet.setValueTextSize(10f);
        barDataSet.setValueTextColor(Color.BLACK);

        barDataSet.setValueFormatter((value, entry, dataSetIndex, viewPortHandler) -> {
            if (value == 0f) return "";
            if (value < 60f) {
                return String.format("%.0f sec", value);
            } else {
                return String.format("%.0f min", value / 60f);
            }
        });

        XAxis xAxis = barChart.getXAxis();
        xAxis.setTextSize(8f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(subjectLabels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(0);
        xAxis.setDrawGridLines(false);

        YAxis yAxisLeft = barChart.getAxisLeft();
        yAxisLeft.setAxisMinimum(0f);
        yAxisLeft.setDrawLabels(false);
        yAxisLeft.setYOffset(50f);
        yAxisLeft.setEnabled(false);

        YAxis yAxisRight = barChart.getAxisRight();
        yAxisRight.setDrawLabels(false);
        yAxisRight.setEnabled(false);
        yAxisRight.setAxisMaximum(1f);


        barChart.setExtraOffsets(15f, 0f, 15f, 0f);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);

        barChart.setScaleEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);

        barChart.invalidate();
    }

    private float parseTimeToSeconds(String time) {
        try {
            String[] parts = time.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);

            return hours * 3600 + minutes * 60 + seconds;
        } catch (Exception e) {
            Log.e("TimeParseError", "Failed to parse time: " + time);
            return 0f;
        }
    }
    private void showMonthlyGoalsDialog() {
        Log.d("Monthly goals", "showMonthlygoalsdialog clicked");
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_monthly_goals);
        Objects.requireNonNull(dialog.getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);

        EditText goalTimeEditText = dialog.findViewById(R.id.goalTimeEditText);
        EditText subjectEditText = dialog.findViewById(R.id.subjectwindow);
        Button saveButton = dialog.findViewById(R.id.save_button);
        Button cancelButton = dialog.findViewById(R.id.cancel_button);

        TextView tag_selection_name = dialog.findViewById(R.id.tag_selection_name);

        if (tagIndex != -1) {
            if (firebaseuserboolean) {
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("subjectnames");
                Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

                databaseReference.orderByChild("tagIndex")
                        .equalTo(tagIndex)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        String subjectName = snapshot.child("subjectName").getValue(String.class);
                                        if (subjectName != null) {
                                            subjectEditText.setText(subjectName);
                                            break;
                                        }
                                    }
                                } else {
                                    Log.w("StudyActivity", "No matching subject name found for the given tagIndex in Firebase.");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.w("StudyActivity", "loadSubject:onCancelled", databaseError.toException());
                            }
                        });
            } else {
                SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();
                String query = "SELECT " + DatabaseHelper.COLUMN_SUBJECT_NAME_TAG + " FROM " + DatabaseHelper.TABLE_SUBJECT_NAMES + " WHERE " + DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT + " = ?";
                Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(tagIndex)});

                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SUBJECT_NAME_TAG);
                    if (columnIndex >= 0) {
                        String subjectName = cursor.getString(columnIndex);
                        subjectEditText.setText(subjectName);
                    } else {
                        Log.w("StudyActivity", "Column 'subject_name_tag' not found in SQLite.");
                    }
                } else {
                    Log.w("StudyActivity", "No matching subject name found for the given tagIndex in SQLite.");
                }

                cursor.close();
                db.close();
            }
        }

        for (int i = 0; i <= 11; i++) {
            int tagId = getResources().getIdentifier("tag" + i, "id", getPackageName());
            ImageView tag = dialog.findViewById(tagId);
            if (tag != null) {
                tag.setTag(tags[i]);
                tag.setOnClickListener(v -> tagAssignment(tag, subjectEditText, tag_selection_name));
            }
        }

        saveButton.setOnClickListener(v -> {
            UserActivity.playSaveSound(this);
            String subjectName = subjectEditText.getText().toString().trim();

            if (subjectName.isEmpty()) {
                Toast.makeText(StudyActivity.this, "Please enter a subject name.", Toast.LENGTH_SHORT).show();
                return;  
            }
            
            if (tagIndex == -1) {
                Toast.makeText(StudyActivity.this, "Please select a tag.", Toast.LENGTH_SHORT).show();
                return;  
            }

            SharedPreferences sharedPreferences = getSharedPreferences("study_preferences", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("subjectName", subjectName);
            editor.putInt("tagIndex", tagIndex);
            editor.apply();

            SQLiteDatabase db = new DatabaseHelper(this).getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_SUBJECT_NAME_TAG, subjectName);
            values.put(DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT, tagIndex);

            int rowsUpdated = db.update(DatabaseHelper.TABLE_SUBJECT_NAMES, values,
                    DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT + " = ?", new String[]{String.valueOf(tagIndex)});

            if (rowsUpdated > 0) {
                Toast.makeText(StudyActivity.this, "Subject updated in SQLite", Toast.LENGTH_SHORT).show();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } else {
                values.put(DatabaseHelper.COLUMN_TAG_INDEX_SUBJECT, tagIndex);
                db.insert(DatabaseHelper.TABLE_SUBJECT_NAMES, null, values);
                Toast.makeText(StudyActivity.this, "New subject added to SQLite", Toast.LENGTH_SHORT).show();
            }

            db.close();

            try {
                int goalTime = Integer.parseInt(goalTimeEditText.getText().toString());
                saveMonthlyGoal(subjectName, goalTime);
            } catch (NumberFormatException e) {
                goalTimeEditText.setError("Enter a valid number");
                return;
            }

            saveTagIndex(tagIndex);

            Toast.makeText(StudyActivity.this, "Subject updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            UserActivity.playCancelSound(this);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveMonthlyGoal(String subjectName, int goalTime) {
        String monthYear = new SimpleDateFormat("MM.yyyy", Locale.getDefault()).format(new Date());
        
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_MONTH_YEAR, monthYear);
        values.put(DatabaseHelper.COLUMN_SUBJECT_NAME_GOAL, subjectName);
        values.put(DatabaseHelper.COLUMN_GOAL_TIME, goalTime);

        Log.d("GoalDebug", "Saving goal for " + subjectName +
                ", time: " + goalTime +
                ", month: " + monthYear);
        db.insertWithOnConflict(DatabaseHelper.TABLE_MONTHLY_GOALS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        getDataProgressSubjects();
    }

    public void fetchMonthlyGoalsFromSQLite(HashMap<String, Float> goalMap) {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH) +1 ;
        int currentYear = calendar.get(Calendar.YEAR);
        String currentMonthYear = String.format("%02d.%d", currentMonth, currentYear);
        Log.d("GoalDebug", "Querying for month: " + currentMonthYear);

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_MONTHLY_GOALS,
                null,
                DatabaseHelper.COLUMN_MONTH_YEAR + "=?",
                new String[]{currentMonthYear},
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int subjectNameColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SUBJECT_NAME_GOAL);
            int goalColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_GOAL_TIME);

            if (subjectNameColumnIndex >= 0 && goalColumnIndex >= 0) {
                do {
                    String subjectName = cursor.getString(subjectNameColumnIndex);
                    float goal = cursor.getFloat(goalColumnIndex);
                    if (subjectName.length() > 9) {
                        subjectName = subjectName.substring(0, 6) + "...";
                    }

                    goalMap.put(subjectName, goal);
                    Log.d("SQLiteGoal", "Subject: " + subjectName + ", Goal: " + goal);
                } while (cursor.moveToNext());
            } else {
                Log.e("SQLiteGoal", "Invalid column indexes in the cursor");
            }
            cursor.close();
        } else {
            Log.w("SQLiteGoal", "No goals found for month: " + currentMonthYear);
        }
    }

    public void updateStudyChart(HashMap<String, Float> dataMap, HashMap<String, Float> goalMap,
                                 HashMap<String, Integer> subjectColorMap) {
        fetchMonthlyGoalsFromSQLite(goalMap);
        boolean hasData = false;
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<String> subjectLabels = new ArrayList<>();
        ArrayList<Integer> barColors = new ArrayList<>();

        Log.d("DataMap", "DataMap: " + dataMap.toString());
        Log.d("GoalMap", "GoalMap: " + goalMap.toString());

        for (Map.Entry<String, Float> goalEntry : goalMap.entrySet()) {
            String subjectName = goalEntry.getKey();
            float goal = goalEntry.getValue();
            float studyTime = dataMap.getOrDefault(subjectName, 0f);

            Log.d("StudyData", "Subject: " + subjectName + ", Study Time: " + studyTime + ", Goal: " + goal);



            if (studyTime > 0f || goal > 0f) {
                hasData = true;
                Log.d("VisibilityCheck", "Data found for subject: " + subjectName);
            }

            if (studyTime >= goal) {
                barEntries.add(new BarEntry(subjectLabels.size(), new float[]{goal, 0})); // Full bar with subject color
                barColors.add(Color.GRAY);
                barColors.add(subjectColorMap.getOrDefault(subjectName, Color.GRAY)); // Add subject color
                // Gray for the remaining part of the bar (no remaining time)
            } else {
                // If study time is less than the goal, show the study time and the remaining time
                barEntries.add(new BarEntry(subjectLabels.size(), new float[]{studyTime, goal - studyTime})); // Partial bar
                barColors.add(Color.GRAY);
                barColors.add(subjectColorMap.getOrDefault(subjectName, Color.GRAY)); // Study time color
                // Remaining time color
            }

            subjectLabels.add(subjectName);
        }

        Log.d("VisibilityCheck", "hasData: " + hasData);

        if (!hasData) {
            progressionstudyTable.setVisibility(View.GONE);
            Button monthlygoalbutton = findViewById(R.id.monthlygoalbutton);
            monthlygoalbutton.setVisibility(View.VISIBLE);
            monthlygoalbutton.setOnClickListener(v -> showMonthlyGoalsDialog());

            TextView studysessionTextView = findViewById(R.id.studysessionTextView);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) studysessionTextView.getLayoutParams();
            params.topToBottom = R.id.monthlygoalbutton;
            studysessionTextView.setLayoutParams(params);
            return;
        } else {
            progressionstudyTable.setVisibility(View.VISIBLE);
            Button monthlygoalbutton = findViewById(R.id.monthlygoalbutton);
            monthlygoalbutton.setVisibility(View.GONE);
            TextView studysessionTextView = findViewById(R.id.studysessionTextView);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) studysessionTextView.getLayoutParams();
            params.topToBottom = R.id.progressionstudyTable;
            studysessionTextView.setLayoutParams(params);
        }

        progressionstudyTable.setClickable(true);
        progressionstudyTable.setFocusable(true);
        progressionstudyTable.setOnClickListener(v -> showMonthlyGoalsDialog());

        // Reverse to make sure the first subjects are at the bottom
        Collections.reverse(barEntries);
        Collections.reverse(barColors);

        BarDataSet barDataSet = new BarDataSet(barEntries, "");
        barDataSet.setStackLabels(new String[]{"Study Time", "Goal"});
        barDataSet.setColors(barColors);

        BarData barData = new BarData(barDataSet);
        progressionstudyTable.setData(barData);
        barDataSet.setDrawValues(false);

        barDataSet.setValueTextSize(9f);

        XAxis xAxis = progressionstudyTable.getXAxis();
        progressionstudyTable.setExtraBottomOffset(20f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(subjectLabels));
        xAxis.setGranularity(1f);
        xAxis.setTextSize(12f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);

        YAxis yAxisLeft = progressionstudyTable.getAxisLeft();
        yAxisLeft.setAxisMinimum(0f);
        yAxisLeft.setDrawGridLines(false);
        yAxisLeft.setEnabled(false);

        YAxis yAxisRight = progressionstudyTable.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(false);

        progressionstudyTable.getDescription().setEnabled(false);
        progressionstudyTable.getLegend().setEnabled(false);

        progressionstudyTable.invalidate();
    }
}