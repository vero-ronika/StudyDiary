package com.veroronika.studyplanner;

import static com.veroronika.studyplanner.DiariesAdapter.getLineCount;
import static com.veroronika.studyplanner.database.DatabaseHelper.COLUMN_DATE_DIARY;
import static com.veroronika.studyplanner.database.DatabaseHelper.COLUMN_DATE_REMINDER;
import static com.veroronika.studyplanner.database.DatabaseHelper.COLUMN_SUBJECT_NAME;
import static com.veroronika.studyplanner.database.DatabaseHelper.COLUMN_TEXT_REMINDER;
import static com.veroronika.studyplanner.database.DatabaseHelper.COLUMN_TIME_DIARY;
import static com.veroronika.studyplanner.database.DatabaseHelper.COLUMN_TIME_REMINDER;
import static com.veroronika.studyplanner.database.DatabaseHelper.TABLE_STUDY;

import com.veroronika.studyplanner.database.DatabaseHelper;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import java.util.ArrayList;
import java.util.List;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

public class  MainActivity extends AppCompatActivity {
    public boolean firebaseuserboolean;
    private DatabaseHelper databaseHelper;

    private Dialog dialog;

    private FirebaseUserStatusListener firebaseUserStatusListener;

    private boolean calendarinitboolean = false;

    private BarChart barChart;
    String userId;

    Button addreminderbutton, revisebutton, startstudybutton, reminderbutton;
    TextView dots, remindermainmenu, studycalendarTextView;

    BottomNavigationView bottomNavigationView;
    FirebaseAuth mAuth;

    private CustomCalendarView customCalendarView;

    private String mood = null;
    private String rating = null;

    ArrayList barArrayList;

    TextView diarytext;
    Button diarymoodbutton, diaryratingbutton, diarysavebutton;

    final List<Pair<Long, Pair<Integer, String>>> reminderDetails = new ArrayList<>();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);
        databaseHelper = new DatabaseHelper(this);

        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        customCalendarView = findViewById(R.id.studycalendar);

        initViews();
        barChart = findViewById(R.id.statisticstable);

        setupBottomNavigationView();

        remindermainmenu = findViewById(R.id.remindermainmenu);
        addreminderbutton = findViewById(R.id.addreminderbutton);
        reminderbutton = findViewById(R.id.reminderbutton);
        revisebutton = findViewById(R.id.revisebutton);

        diarytext = findViewById(R.id.diarytext);
        diarymoodbutton = findViewById(R.id.diarymoodbutton);
        diaryratingbutton = findViewById(R.id.diaryratingbutton);
        diarysavebutton = findViewById(R.id.diarysavebutton);

        diarytext.setOnClickListener(v -> showTextDialog());
        diarytext.setMovementMethod(new ScrollingMovementMethod());
        diarytext.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        diarymoodbutton.setOnClickListener(v -> showMoodDialog());
        diaryratingbutton.setOnClickListener(v -> showRatingDialog());
        diarysavebutton.setOnClickListener(v -> saveDiaryEntry());

        View.OnClickListener buttonClickListener = v -> {
            UserActivity.playButtonSound(this);
            Intent intent = new Intent(MainActivity.this, StudyActivity.class);
            intent.putExtra("CALL_REMINDER_DIALOG", true);
            startActivity(intent);
        };

        addreminderbutton.setOnClickListener(buttonClickListener);
        reminderbutton.setOnClickListener(buttonClickListener);

        revisebutton.setOnClickListener(v -> {
            UserActivity.playButtonSound(this);
            Intent intent = new Intent(MainActivity.this, StudyActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (customCalendarView != null) {
            firebaseUserStatusListener = customCalendarView;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();

            if (isConnectedToInternet()) {
                if (!firebaseuserboolean) {
                    firebaseuserboolean = true;
                }
                loadHeaderReminderFirebase();
            } else {
                firebaseuserboolean = false;
                loadHeaderReminderSQL();
            }
        } else {
            firebaseuserboolean = false;
            loadHeaderReminderSQL();
        }

        if (firebaseUserStatusListener != null) {
            firebaseUserStatusListener.onFirebaseUserStatusUpdated(firebaseuserboolean);
        }

        getDataStatistics();

        Log.d("MainActivity", "firebase user: " + firebaseuserboolean);
    }

    public boolean getFirebaseUserBoolean() {
        return firebaseuserboolean;
    }

    public interface FirebaseUserStatusListener {
        void onFirebaseUserStatusUpdated(boolean firebaseUserStatus);
    }


    private boolean isConnectedToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }


    private void initViews() {
        revisebutton = findViewById(R.id.revisebutton);
        startstudybutton = findViewById(R.id.startstudybutton);
        reminderbutton = findViewById(R.id.reminderbutton);

        dots = findViewById(R.id.dots);
        remindermainmenu = findViewById(R.id.remindermainmenu);
        studycalendarTextView = findViewById(R.id.studycalendarTextView);

        bottomNavigationView = findViewById(R.id.bottomnavigationview);
        int selectedItemId = R.id.navigation_home;

        bottomNavigationView.setSelectedItemId(selectedItemId);
        ColorStateList colorStateList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.main_default_dark)
        );
        bottomNavigationView.setItemActiveIndicatorColor(colorStateList);
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent;
            switch (Objects.requireNonNull(item.getTitle()).toString()) {
                case "Home":
                    UserActivity.playButtonSound(this);
                    return true;

                case "Study":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(MainActivity.this, StudyActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;

                case "Diary":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(MainActivity.this, DiaryActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;

                case "User":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(MainActivity.this, UserActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                default:
                    return false;
            }
        });
    }

    private void startStudyActivity() {
        Intent intent = new Intent(MainActivity.this, StudyActivity.class);
        startActivity(intent);
    }

    private int getColorForMood(String mood) {
        switch (mood.toLowerCase()) {
            case "angry":
                return Color.parseColor("#700303");
            case "sad":
                return Color.parseColor("#031070");
            case "neutral":
                return Color.parseColor("#6F6971");
            case "calm":
                return Color.parseColor("#227003");
            case "happy":
                return Color.parseColor("#BD8004");
            default:
                return Color.parseColor("#E0E0E0");
        }
    }

    private int getColorForRating(String rating) {
        switch (rating) {
            case "1":
                return Color.parseColor("#0A9DFF");
            case "2":
                return Color.parseColor("#0087E0");
            case "3":
                return Color.parseColor("#006EB8");
            case "4":
                return Color.parseColor("#00568F");
            case "5":
                return Color.parseColor("#003D66");
            default:
                return Color.GRAY;
        }
    }

    private void showMoodDialog() {
        UserActivity.playButtonSound(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Select Mood");

        String[] moodOptions = {"Happy", "Calm", "Neutral", "Sad", "Angry"};
        builder.setItems(moodOptions, (dialog, which) -> {
            mood = moodOptions[which];
            diarymoodbutton.setText(mood);
            diarymoodbutton.setBackgroundColor(getColorForMood(mood));
        });

        builder.create().show();
    }

    private void showRatingDialog() {
        UserActivity.playButtonSound(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Rate Your Day");

        String[] ratingOptions = {"1", "2", "3", "4", "5"};
        builder.setItems(ratingOptions, (dialog, which) -> {
            rating = ratingOptions[which];
            diaryratingbutton.setText(rating);
            diaryratingbutton.setBackgroundColor(getColorForRating(rating));
        });

        builder.create().show();
    }

    private void showTextDialog() {
        if (dialog != null && dialog.isShowing()) {
            return;
        }

        dialog = new Dialog(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_text, null);
        dialog.setContentView(dialogView);

        EditText dialogEditText = dialogView.findViewById(R.id.diaryText);

        String textInTextView = diarytext.getText().toString();
        if (textInTextView.equals("Tap to edit diary text")) {
            dialogEditText.setText("");
        } else {
            dialogEditText.setText(textInTextView);
        }
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button saveButton = dialogView.findViewById(R.id.save_button);

        cancelButton.setOnClickListener(v -> {
            UserActivity.playCancelSound(this);
            dialog.dismiss();
        });

        saveButton.setOnClickListener(v -> {
            UserActivity.playSaveSound(this);
            diarytext.setText(dialogEditText.getText().toString());
            diarytext.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.black)));
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveDiaryEntry() {
        String text = diarytext.getText().toString();

        if (text.isEmpty() || mood == null || rating == null) {
            Toast.makeText(MainActivity.this, "Please fill in all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentDate = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        if (firebaseuserboolean) {
            String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("diary_entries").child(userId);
            String entryId = databaseReference.push().getKey();

            if (entryId != null) {
                HashMap<String, Object> diaryData = new HashMap<>();
                diaryData.put("date", currentDate);
                diaryData.put("time", currentTime);
                diaryData.put("text", text);
                diaryData.put("mood", mood);
                diaryData.put("rating", rating);

                databaseReference.child(entryId).setValue(diaryData).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        UserActivity.playSaveSound(this);
                        Log.d("MainActivity", "Diary entry added to Firebase: " + entryId);
                        Toast.makeText(MainActivity.this, "Diary entry is saved to your Account.", Toast.LENGTH_SHORT).show();
                        diarytext.setText("Tap to edit diary text");
                        diarymoodbutton.setText("Mood");
                        diarymoodbutton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.second_button_color)));
                        diaryratingbutton.setText("Rating");
                        diaryratingbutton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.second_button_color)));
                    } else {
                        Log.w("MainActivity", "Failed to add diary to Firebase", task.getException());
                        Toast.makeText(MainActivity.this, "There's been an error in saving.", Toast.LENGTH_SHORT).show();
                        firebaseuserboolean = false;
                        saveDiaryEntry();
                    }
                });
            }
        } else {
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_DATE_DIARY, currentDate);
            values.put(COLUMN_TIME_DIARY, currentTime);
            values.put(DatabaseHelper.COLUMN_TEXT_DIARY, text);
            values.put(DatabaseHelper.COLUMN_MOOD_DIARY, mood);
            values.put(DatabaseHelper.COLUMN_RATING_DIARY, rating);

            long newRowId = db.insert(DatabaseHelper.TABLE_DIARY, null, values);

            if (newRowId != -1) {
                UserActivity.playSaveSound(this);
                Log.d("MainActivity", "Diary entry saved to SQLite");
                Toast.makeText(MainActivity.this, "Diary entry is saved in your device", Toast.LENGTH_SHORT).show();
                diarytext.setText("Tap to edit diary text");
                diarymoodbutton.setText("Mood");
                diarymoodbutton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.second_button_color)));
                diaryratingbutton.setText("Rating");
                diaryratingbutton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.second_button_color)));
            } else {
                Log.w("MainActivity", "Failed to save diary entry to SQLite.");
                Toast.makeText(MainActivity.this, "There's been an error in saving.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private ArrayList<String> dateLabels;

    private void getDataStatistics() {
        barArrayList = new ArrayList<>();
        dateLabels = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        HashMap<String, Float> dataMap = new HashMap<>();
        calendar.add(Calendar.DAY_OF_YEAR, -6);
        String startDate = dateFormat.format(calendar.getTime());
        String endDate = dateFormat.format(Calendar.getInstance().getTime());
        Log.d("DateRange", "Start: " + startDate + ", End: " + endDate);

        if (firebaseuserboolean) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Log.e("Firebase", "User not logged in!");
                return;
            }
            String userId = user.getUid();
            Log.d("Firebase", "User ID: " + userId);

            DatabaseReference database = FirebaseDatabase.getInstance().getReference("study_sessions").child(userId);
            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Log.d("Firebase", "DataSnapshot: " + dataSnapshot.toString());

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String dateString = snapshot.child("date").getValue(String.class);
                        String stopwatchTime = snapshot.child("stopwatch_time").getValue(String.class);
                        Log.d("Firebase", "Raw Date: " + dateString + ", Raw Stopwatch Time: " + stopwatchTime);

                        if (dateString != null && stopwatchTime != null && isDateInRange(dateString, startDate, endDate)) {
                            float timeInSeconds = parseTimeToSeconds(stopwatchTime);
                            Log.d("Firebase", "Parsed Time (seconds): " + timeInSeconds);

                            dataMap.put(dateString, dataMap.getOrDefault(dateString, 0f) + timeInSeconds);
                        }
                    }
                    Log.d("Firebase", "Final Data Map: " + dataMap.toString());
                    updateStatisticsUI(dataMap, dateFormat, displayFormat);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("Firebase", "Error loading data: " + databaseError.getMessage());
                }
            });
        } else {
            String query = "SELECT date, stopwatch_time FROM StudySession";
            Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(query, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int dateColumnIndex = cursor.getColumnIndex("date");
                    int stopwatchTimeColumnIndex = cursor.getColumnIndex("stopwatch_time");
                    if (dateColumnIndex != -1 && stopwatchTimeColumnIndex != -1) {
                        String dateString = cursor.getString(dateColumnIndex);
                        String stopwatchTime = cursor.getString(stopwatchTimeColumnIndex);
                        Log.d("Database", "SQLite Date: " + dateString + ", Stopwatch Time: " + stopwatchTime);

                        if (isDateInRange(dateString, startDate, endDate)) {
                            float timeInSeconds = parseTimeToSeconds(stopwatchTime);
                            Log.d("Database", "Parsed Time (seconds): " + timeInSeconds);
                            dataMap.put(dateString, dataMap.getOrDefault(dateString, 0f) + timeInSeconds);
                        }
                    }
                }
                cursor.close();
            }
            Log.d("Database", "Final Data Map: " + dataMap.toString());
            updateStatisticsUI(dataMap, dateFormat, displayFormat);
        }
    }

    @SuppressLint("DefaultLocale")
    private void updateStatisticsUI(HashMap<String, Float> dataMap, SimpleDateFormat dateFormat, SimpleDateFormat displayFormat) {
        if (dataMap.isEmpty()) {

            findViewById(R.id.statisticsTextView).setVisibility(View.INVISIBLE);
            findViewById(R.id.statisticstable).setVisibility(View.INVISIBLE);
            findViewById(R.id.startstudybutton).setVisibility(View.INVISIBLE);
            findViewById(R.id.todaysdiaryentryTextView).setVisibility(View.VISIBLE);
            TextView todaysdiaryentryTextView = findViewById(R.id.todaysdiaryentryTextView);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) todaysdiaryentryTextView.getLayoutParams();
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            todaysdiaryentryTextView.setLayoutParams(params);
            displayNoDataMessage(barChart);
        } else {
            Calendar calendar = Calendar.getInstance();
            for (int i = 0; i <= 6; i++) {
                String dateString = dateFormat.format(calendar.getTime());
                float timeValue = dataMap.getOrDefault(dateString, 0f);


                if (timeValue == 0f) {
                    barArrayList.add(new BarEntry(i, 0f));
                } else {
                    timeValue /= 60f;

                    barArrayList.add(new BarEntry(i, timeValue));
                }
                dateLabels.add(displayFormat.format(calendar.getTime()));
                calendar.add(Calendar.DAY_OF_YEAR, -1);
            }
            if (!barArrayList.isEmpty()) {
                BarDataSet barDataSet = new BarDataSet(barArrayList, "Stopwatch Time!");
                BarData barData = new BarData(barDataSet);
                barChart.setData(barData);
                barDataSet.setColor(Color.parseColor("#F9CB40"));
                barDataSet.setValueTextColor(Color.BLACK);
                barDataSet.setValueTextSize(12f);
                barDataSet.setDrawValues(true);
                barDataSet.setValueFormatter((value, entry, dataSetIndex, viewPortHandler) -> value == 0f ? "" : String.format("%.0f min", value));
                XAxis xAxis = barChart.getXAxis();
                xAxis.setTextSize(10f);
                xAxis.setValueFormatter(new IndexAxisValueFormatter(dateLabels));
                xAxis.setGranularity(1f);
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setDrawGridLines(false);
                YAxis yAxisLeft = barChart.getAxisLeft();
                yAxisLeft.setAxisMinimum(0f);
                yAxisLeft.setDrawGridLines(false);
                yAxisLeft.setEnabled(false);
                YAxis yAxisRight = barChart.getAxisRight();
                yAxisRight.setDrawGridLines(false);
                yAxisRight.setEnabled(false);
                barChart.getDescription().setEnabled(false);
                barChart.getLegend().setEnabled(false);
                barChart.setScaleEnabled(false);
                barChart.setPinchZoom(false);
                barChart.setDoubleTapToZoomEnabled(false);
                barChart.invalidate();
            } else {
                displayNoDataMessage(barChart);
            }
        }
    }

    private boolean isDateInRange(String dateString, String startDate, String endDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            Date date = dateFormat.parse(dateString);
            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);

            assert date != null;
            return !date.before(start) && !date.after(end);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
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

    public static void displayNoDataMessage(BarChart barChart) {
        if (barChart.getData() == null || barChart.getData().getDataSetCount() == 0) {
            barChart.setNoDataText("No data found");
            barChart.setNoDataTextColor(Color.BLACK);
        } else {
            barChart.clear();
        }
    }

    public static void displayNoDataMessage(HorizontalBarChart barChart) {
        if (barChart.getData() == null || barChart.getData().getDataSetCount() == 0) {
            barChart.setNoDataText("No data found");
            barChart.setNoDataTextColor(Color.BLACK);
        } else {
            barChart.clear();
        }
    }

    private void loadHeaderReminderFirebase() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("study_entries");

        DatabaseReference userEntriesReference = databaseReference.child(userId);
        userEntriesReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    loadHeaderReminderSQL();
                    return;
                }

                String closestSubject = null;
                String closestText = null;
                long closestTimestamp = Long.MAX_VALUE;
                long closestDate = Long.MAX_VALUE;
                String closestTime = null;
                long todayMillis = System.currentTimeMillis();


                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(todayMillis);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long startOfToday = calendar.getTimeInMillis();

                for (DataSnapshot entry : snapshot.getChildren()) {
                    String subjectName = entry.child("subjectName").getValue(String.class);
                    String textReminder = entry.child("textReminder").getValue(String.class);
                    String dateReminderStr = entry.child("dateReminder").getValue(String.class);
                    Integer tagIndex = entry.child("tagIndex").getValue(Integer.class);
                    String timeReminderStr = entry.child("timeReminder").getValue(String.class);

                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("d.MM.yyyy", Locale.getDefault());
                        assert dateReminderStr != null;
                        Date dateReminder = dateFormat.parse(dateReminderStr);
                        if (dateReminder != null) {
                            long dateMillis = dateReminder.getTime();

                            if (dateMillis > todayMillis) {
                                reminderDetails.add(new Pair<>(dateMillis, new Pair<>(tagIndex, timeReminderStr)));

                                if (dateMillis < closestDate) {
                                    closestDate = dateMillis;
                                    closestSubject = subjectName;
                                    closestText = textReminder;
                                    closestTime = timeReminderStr;
                                }

                                if (dateMillis >= startOfToday) {
                                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                    Date timeReminderDate = timeFormat.parse(timeReminderStr);
                                    if (timeReminderDate != null) {
                                        long timeReminderMillis = timeReminderDate.getTime();
                                        long fullReminderTimestamp = dateMillis + (timeReminderMillis % (24 * 60 * 60 * 1000));

                                        if (Math.abs(fullReminderTimestamp - todayMillis) < Math.abs(closestTimestamp - todayMillis)) {
                                            closestTimestamp = fullReminderTimestamp;
                                            closestSubject = subjectName;
                                            closestText = textReminder;
                                            closestTime = timeReminderStr;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (ParseException e) {
                        Log.e("ReminderParseError", "Invalid date: " + dateReminderStr, e);
                    }
                }

                if (closestSubject != null && closestText != null && closestTime != null) {
                    updateReminderTextView(closestSubject, closestText, closestTimestamp, closestTime);
                }

                if (!calendarinitboolean) {
                    customCalendarView.setReminderDates(reminderDetails);
                    calendarinitboolean = true;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load reminders from Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadHeaderReminderSQL() {
        String query = "SELECT " +
                COLUMN_SUBJECT_NAME + ", " +
                COLUMN_TEXT_REMINDER + ", " +
                COLUMN_DATE_REMINDER + ", " +
                COLUMN_TIME_REMINDER + ", " +
                DatabaseHelper.COLUMN_TAG_INDEX +
                " FROM " + TABLE_STUDY;

        Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(query, null);

        if (cursor == null || cursor.getCount() == 0) {
            findViewById(R.id.addreminderbutton).setVisibility(View.VISIBLE);
            findViewById(R.id.reminderbutton).setVisibility(View.INVISIBLE);
            findViewById(R.id.revisebutton).setVisibility(View.INVISIBLE);

            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        int subjectIndex = cursor.getColumnIndex(COLUMN_SUBJECT_NAME);
        int textIndex = cursor.getColumnIndex(COLUMN_TEXT_REMINDER);
        int dateIndex = cursor.getColumnIndex(COLUMN_DATE_REMINDER);
        int timeIndex = cursor.getColumnIndex(COLUMN_TIME_REMINDER);
        int tagIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TAG_INDEX);
        if (subjectIndex == -1 || textIndex == -1 || dateIndex == -1 || timeIndex == -1) {
            cursor.close();
            throw new IllegalArgumentException("Invalid column name in query.");
        }

        String closestSubject = null;
        String closestText = null;
        long closestTimestamp = Long.MAX_VALUE;
        long closestDate = Long.MAX_VALUE;
        String closestTime = null;
        long todayMillis = System.currentTimeMillis();


        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(todayMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfToday = calendar.getTimeInMillis();

        while (cursor.moveToNext()) {
            String subjectName = cursor.getString(subjectIndex);
            String textReminder = cursor.getString(textIndex);
            String dateReminderStr = cursor.getString(dateIndex);
            String timeReminderStr = cursor.getString(timeIndex);
            int tagIndexValue = cursor.getInt(tagIndex);

            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.y", Locale.getDefault());
                Date dateReminder = dateFormat.parse(dateReminderStr);
                if (dateReminder != null) {
                    long dateMillis = dateReminder.getTime();

                    if (dateMillis > todayMillis) {
                        reminderDetails.add(new Pair<>(dateMillis, new Pair<>(tagIndexValue, timeReminderStr)));

                        if (dateMillis < closestDate) {
                            closestDate = dateMillis;
                            closestSubject = subjectName;
                            closestText = textReminder;
                            closestTime = timeReminderStr;
                        }

                        if (dateMillis >= startOfToday) {
                            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                            Date timeReminderDate = timeFormat.parse(timeReminderStr);
                            if (timeReminderDate != null) {
                                long timeReminderMillis = timeReminderDate.getTime();
                                long fullReminderTimestamp = dateMillis + (timeReminderMillis % (24 * 60 * 60 * 1000));

                                if (Math.abs(fullReminderTimestamp - todayMillis) < Math.abs(closestTimestamp - todayMillis)) {
                                    closestTimestamp = fullReminderTimestamp;
                                    closestSubject = subjectName;
                                    closestText = textReminder;
                                    closestTime = timeReminderStr;
                                }
                            }
                        }
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        cursor.close();

        if (!calendarinitboolean) {
            customCalendarView.setReminderDates(reminderDetails);
            calendarinitboolean = true;
        }

        if (closestSubject != null && closestText != null && closestTime != null) {
            updateReminderTextView(closestSubject, closestText, closestTimestamp, closestTime);
        }
    }

    private void updateReminderTextView(String subjectName, String textReminder, long dateMillis, String timeReminder) {
        ConstraintLayout headerLayout = findViewById(R.id.headerLayout);
        ConstraintLayout headerWindow = findViewById(R.id.headerWindow);

        long currentMillis = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentMillis);

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfToday = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        long startOfTomorrow = calendar.getTimeInMillis();

        subjectName = subjectName.length() > 30 ? subjectName.substring(0, 30) : subjectName;
        textReminder = textReminder.length() > 30 ? textReminder.substring(0, 30) : textReminder;

        String formattedText = "";
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date timeDate = timeFormat.parse(timeReminder);
            assert timeDate != null;
            String formattedTime = timeFormat.format(timeDate);

            if (dateMillis >= startOfToday && dateMillis < startOfTomorrow) {
                formattedText = String.format(Locale.getDefault(), "%s %s today at %s", subjectName, textReminder, formattedTime);
            } else if (dateMillis >= startOfTomorrow && dateMillis < startOfTomorrow + 24 * 60 * 60 * 1000) {
                formattedText = String.format(Locale.getDefault(), "%s %s tomorrow at %s", subjectName, textReminder, formattedTime);
            } else {
                long daysDiff = (dateMillis - currentMillis) / (1000 * 60 * 60 * 24);
                formattedText = String.format(Locale.getDefault(), "%s %s in %d days", subjectName, textReminder, daysDiff);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int lineCount = getLineCount(formattedText, new StringBuilder(), 20);

        // Base height for each view in dp
        int baseHeaderLayoutHeightDp = 260; // Base height for headerLayout
        int baseHeaderWindowHeightDp = 200; // Base height for headerWindow

        // Add extra height based on line count (10 dp per extra line)
        int additionalHeightDp = 13 * (lineCount - 1);

        // Calculate the total height in dp for both layouts
        int totalHeaderLayoutHeightDp = baseHeaderLayoutHeightDp + additionalHeightDp;
        int totalHeaderWindowHeightDp = baseHeaderWindowHeightDp + additionalHeightDp;

        // Convert dp to pixels
        int newHeaderLayoutHeight = dpToPx(totalHeaderLayoutHeightDp);
        int newHeaderWindowHeight = dpToPx(totalHeaderWindowHeightDp);

        // Update layout parameters with dp-based heights
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) headerLayout.getLayoutParams();
        layoutParams.height = newHeaderLayoutHeight;
        headerLayout.setLayoutParams(layoutParams);

        ConstraintLayout.LayoutParams windowParams = (ConstraintLayout.LayoutParams) headerWindow.getLayoutParams();
        windowParams.height = newHeaderWindowHeight;
        headerWindow.setLayoutParams(windowParams);

        // Set the reminder text
        remindermainmenu.setText(formattedText);
    }

    // Helper function to convert dp to px
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }
}