package com.veroronika.studyplanner;

import static com.veroronika.studyplanner.database.DatabaseHelper.COLUMN_DATE_DIARY;
import static com.veroronika.studyplanner.database.DatabaseHelper.COLUMN_TIME_DIARY;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veroronika.studyplanner.database.DatabaseHelper;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class DiaryActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    public boolean firebaseuserboolean;

    private Dialog dialog;

    private RecyclerView diariesRecyclerView;
    private DiariesAdapter diariesAdapter;
    private List<String[]> diaryList;

    private List<String> dateList;

    FirebaseAuth mAuth;

    Button diarymoodbutton, diaryratingbutton, diarysavebutton;
    TextView diarytext, nodiariesTextView, currentstreakdiary, dots, moodandratingsTextView, diaryentriesTextView;
    ConstraintLayout mainLayout;
    BottomNavigationView bottomNavigationView;

    private CustomCalendarView customCalendarView;

    final List<Pair<String, Pair<Long, Pair<String, Pair<String, String>>>>> diaryDetails = new ArrayList<>();
    String userId;

    private String mood = null;
    private String rating = null;

    private String changedDate;
    private String changedTime;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);
        databaseHelper = new DatabaseHelper(this);

        setContentView(R.layout.activity_diary);

        mAuth = FirebaseAuth.getInstance();

        initViews();

        setupBottomNavigationView();

        customCalendarView = findViewById(R.id.moodandratingscalendar);

        diariesRecyclerView = findViewById(R.id.diaryRecyclerView);
        if (diariesRecyclerView == null) {
            Log.e("AllDiariesActivity", "RecyclerView not found!");
        }

        diariesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        diariesRecyclerView.setNestedScrollingEnabled(false);

        diariesRecyclerView.setAdapter(diariesAdapter);

        diarytext = findViewById(R.id.typeherewindow);
        diarymoodbutton = findViewById(R.id.diarymoodbutton);
        diaryratingbutton = findViewById(R.id.diaryratingbutton);
        diarysavebutton = findViewById(R.id.diarysavebutton);

        currentstreakdiary = findViewById(R.id.currentstreakdiary);

        diarytext.setOnClickListener(v -> showTextDialog());
        diarytext.setMovementMethod(new ScrollingMovementMethod());
        diarytext.setOnTouchListener((v, event) -> {
            UserActivity.playButtonSound(this);
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
        diarymoodbutton.setOnClickListener(v -> showMoodDialog());
        diaryratingbutton.setOnClickListener(v -> showRatingDialog());
        diarysavebutton.setOnClickListener(v -> saveDiaryEntry());


        Button dateChangeButton = findViewById(R.id.datechangebutton);
        TextView diaryDateWindowTitle = findViewById(R.id.diarydatewindowTitle);

        dateChangeButton.setOnClickListener(v -> {
            UserActivity.playButtonSound(this);
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                                (timeView, hourOfDay, minute) -> {
                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    calendar.set(Calendar.MINUTE, minute);
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy HH:mm", Locale.getDefault());
                                    String formattedDate = dateFormat.format(calendar.getTime());
                                    diaryDateWindowTitle.setText(formattedDate);
                                    diaryDateWindowTitle.setTextSize(16);
                                    String[] dateTimeParts = formattedDate.split(" ");
                                    changedDate = dateTimeParts[0];
                                    changedTime = dateTimeParts[1];
                                },
                                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
                        );
                        timePickerDialog.show();
                    },
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        int selectedItemId = R.id.navigation_diary;

        bottomNavigationView.setSelectedItemId(selectedItemId);

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();

            if (isConnectedToInternet()) {
                if (!firebaseuserboolean) {
                    migrateDiaryDataToFirebase();
                    firebaseuserboolean = true;
                }
                loadDiariesFromFirebase();
                migrateDiaryDataToFirebase();
            } else {
                firebaseuserboolean = false;
                loadDiariesFromSQL();
            }
        } else {
            firebaseuserboolean = false;
            loadDiariesFromSQL();
        }
    }

    private void migrateDiaryDataToFirebase() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String[] projection = {
                DatabaseHelper.COLUMN_DATE_DIARY,
                DatabaseHelper.COLUMN_TIME_DIARY,
                DatabaseHelper.COLUMN_TEXT_DIARY,
                DatabaseHelper.COLUMN_MOOD_DIARY,
                DatabaseHelper.COLUMN_RATING_DIARY
        };

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_DIARY,
                projection,
                null, null, null, null, null, null
        );

        if (cursor.moveToFirst()) {
            do {
                int dateIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE_DIARY);
                int timeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME_DIARY);
                int textIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT_DIARY);
                int moodIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_MOOD_DIARY);
                int ratingIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_RATING_DIARY);

                if (dateIndex >= 0 && timeIndex >= 0 && textIndex >= 0 && moodIndex >= 0 && ratingIndex >= 0) {
                    String date = cursor.getString(dateIndex);
                    String time = cursor.getString(timeIndex);
                    String text = cursor.getString(textIndex);
                    String mood = cursor.getString(moodIndex);
                    int rating = cursor.getInt(ratingIndex);

                    if (firebaseuserboolean) {
                        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("diary_entries").child(userId);
                        String entryId = databaseReference.push().getKey();

                        if (entryId != null) {
                            HashMap<String, Object> diaryData = new HashMap<>();
                            diaryData.put("date", date);
                            diaryData.put("time", time);
                            diaryData.put("text", text);
                            diaryData.put("mood", mood);
                            diaryData.put("rating", (long) rating);


                            databaseReference.child(entryId).setValue(diaryData).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    String whereClause = DatabaseHelper.COLUMN_DATE_DIARY + " = ?";
                                    String[] whereArgs = { date };
                                    db.delete(DatabaseHelper.TABLE_DIARY, whereClause, whereArgs);
                                    Log.d("DiaryActivity", "Diary entry added to Firebase: " + entryId);
                                    Toast.makeText(DiaryActivity.this, "Diary entry is saved to your Account.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.w("DiaryActivity", "Failed to add diary entry to Firebase", task.getException());
                                    Toast.makeText(DiaryActivity.this, "Error saving to Firebase.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
    }


    private boolean isConnectedToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void initViews(){
        moodandratingsTextView = findViewById(R.id.moodandratingsTextView);
        diaryentriesTextView = findViewById(R.id.diaryentriesTextView);
        dots = findViewById(R.id.dots);
        mainLayout = findViewById(R.id.mainLayout);
        nodiariesTextView = findViewById(R.id.nodiariesTextView);

        bottomNavigationView = findViewById(R.id.bottomnavigationview);
        ColorStateList colorStateList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.main_default_dark)
        );
        bottomNavigationView.setItemActiveIndicatorColor(colorStateList);
    }

    private void setupBottomNavigationView(){
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent;
            switch (Objects.requireNonNull(item.getTitle()).toString()) {
                case "Home":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(DiaryActivity.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;

                case "Study":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(DiaryActivity.this, StudyActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;

                case "Diary":
                    UserActivity.playButtonSound(this);
                    return true;

                case "User":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(DiaryActivity.this, UserActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                default:
                    return false;
            }
        });
    }

    private void calculateStreak() {
        int streak = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        if (dateList == null || dateList.isEmpty()) {
            currentstreakdiary.setText("Current streak: 0 days");
            return;
        }

        List<Date> sortedDates = new ArrayList<>();
        try {
            for (String dateStr : dateList) {
                sortedDates.add(sdf.parse(dateStr));
            }
            Collections.sort(sortedDates, Collections.reverseOrder()); 
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();

        
        for (Date diaryDate : sortedDates) {
            if (diaryDate.after(today)) {
                continue; 
            }

            
            if (sdf.format(diaryDate).equals(sdf.format(today))) {
                streak++;
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                today = calendar.getTime();
            } else {
                break; 
            }
        }

        currentstreakdiary.setText("Current streak: " + streak + " days");

        
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("diaryStreak", streak);
        editor.apply();
    }

    private int getColorForMood(String mood) {
        switch (mood.toLowerCase()) {
            case "angry":
                return Color.parseColor("#B71C1C"); // Dark Red
            case "sad":
                return Color.parseColor("#1A237E"); // Dark Blue
            case "neutral":
                return Color.parseColor("#757575"); // Dark Gray
            case "calm":
                return Color.parseColor("#388E3C"); // Dark Green
            case "happy":
                return Color.parseColor("#FBC02D"); // Golden Yellow
            default:
                return Color.parseColor("#D6D6D6"); // Soft Gray
        }
    }

    private int getColorForRating(String rating) {
        switch (rating) {
            case "1":
                return Color.parseColor("#F44336"); // Soft Red
            case "2":
                return Color.parseColor("#FF9800"); // Light Orange
            case "3":
                return Color.parseColor("#FFEB3B"); // Yellow-Green
            case "4":
                return Color.parseColor("#4CAF50"); // Vibrant Green
            case "5":
                return Color.parseColor("#9C27B0"); // Purple
            default:
                return Color.parseColor("#9E9E9E"); // Gray
        }
    }



    private void loadDiariesFromSQL() {
        diaryList = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_DIARY,
                null,
                null,
                null,
                null,
                null,
                DatabaseHelper.COLUMN_DATE_DIARY + " DESC, " + DatabaseHelper.COLUMN_TIME_DIARY + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            int dateColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE_DIARY);
            int textColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT_DIARY);
            int timeColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME_DIARY);
            int moodColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_MOOD_DIARY);
            int ratingColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_RATING_DIARY);

            if (dateColumnIndex >= 0 && textColumnIndex >= 0 && timeColumnIndex >= 0 && moodColumnIndex >= 0 && ratingColumnIndex >= 0) {
                do {
                    String dateDiary = cursor.getString(dateColumnIndex);
                    String textDiary = cursor.getString(textColumnIndex);
                    String timeDiary = cursor.getString(timeColumnIndex);
                    String moodDiary = cursor.getString(moodColumnIndex);
                    String ratingDiary = cursor.getString(ratingColumnIndex);

                    if (dateList == null) {
                        dateList = new ArrayList<>();
                    }

                    if (!dateList.contains(dateDiary)) {
                        dateList.add(dateDiary);
                    }

                    int moodColor = getColorForMood(moodDiary);
                    int ratingColor = getColorForRating(ratingDiary);
                    diaryList.add(new String[]{dateDiary, textDiary, timeDiary, moodDiary, ratingDiary, String.valueOf(moodColor), String.valueOf(ratingColor)});

                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.y", Locale.getDefault());
                        Date dateDiaryd = dateFormat.parse(dateDiary);
                        if (dateDiaryd != null) {
                            long dateMillis = dateDiaryd.getTime();
                            diaryDetails.add(new Pair<>(ratingDiary, new Pair<>(dateMillis, new Pair<>(moodDiary, new Pair<>(timeDiary, textDiary)))));

                        }
                    } catch (ParseException e) {
                        Log.e("AllDiariesActivity", "Date parsing failed for: " + dateDiary, e);
                    }

                } while (cursor.moveToNext());

            } else {
                Log.e("AllDiariesActivity", "Column index is invalid");
            }
        } else {
            nodiariesTextView.setVisibility(View.VISIBLE);
        }
        if (cursor != null) cursor.close();

        customCalendarView.setDiaryDates(diaryDetails);
        calculateStreak();
        updateRecyclerView();
    }

    private void loadDiariesFromFirebase() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("diary_entries").child(userId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                diaryList = new ArrayList<>();

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
                        String dateDiary = snapshot.child("date").getValue(String.class);
                        String textDiary = snapshot.child("text").getValue(String.class);
                        String timeDiary = snapshot.child("time").getValue(String.class);
                        String moodDiary = snapshot.child("mood").getValue(String.class);

                        String ratingDiaryStr = snapshot.child("rating").getValue(String.class);
                        Long ratingDiary = (ratingDiaryStr != null) ? Long.valueOf(ratingDiaryStr) : null;


                        if (dateDiary == null || textDiary == null || timeDiary == null || moodDiary == null || ratingDiary == null) {
                            Log.w("FirebaseData", "Skipping invalid diary entry: " + snapshot.getKey());
                            continue;
                        }

                        Log.d("FirebaseData", "Date: " + dateDiary + ", Text: " + textDiary);

                        if (!dateList.contains(dateDiary)) {
                            dateList.add(dateDiary);
                        }


                        int moodColor = getColorForMood(moodDiary);
                        int ratingColor = getColorForRating(String.valueOf(ratingDiary));


                        diaryList.add(new String[]{
                                dateDiary,
                                textDiary,
                                timeDiary,
                                moodDiary,
                                String.valueOf(ratingDiary),
                                String.valueOf(moodColor),
                                String.valueOf(ratingColor)
                        });

                        try {
                            if (!dateDiary.isEmpty()) {
                                SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.y", Locale.getDefault());
                                Date dateDiaryd = dateFormat.parse(dateDiary);
                                if (dateDiaryd != null) {
                                    long dateMillis = dateDiaryd.getTime();

                                    diaryDetails.add(new Pair<>(ratingDiaryStr, new Pair<>(dateMillis, new Pair<>(moodDiary, new Pair<>(timeDiary, textDiary)))));
                                }
                            } else {
                                Log.w("FirebaseData", "Invalid date format for diary entry: " + dateDiary);
                            }
                        } catch (ParseException e) {
                            Log.e("AllDiariesActivity", "Date parsing failed for: " + dateDiary, e);
                        }
                    }
                } else {
                    Log.w("FirebaseData", "No data found for userId: " + userId);
                    loadDiariesFromSQL();
                }

                customCalendarView.setDiaryDates(diaryDetails);

                calculateStreak();
                updateRecyclerView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("AllDiariesActivity", "loadDiariesEntries:onCancelled", databaseError.toException());
            }
        });
    }


    private void updateRecyclerView() {
        if (diariesAdapter == null) {
            diariesAdapter = new DiariesAdapter(diaryList);
            diariesRecyclerView.setAdapter(diariesAdapter);
            diaryentriesTextView.setVisibility(View.GONE);
        } else {
            diariesAdapter.notifyDataSetChanged();
        }

        diariesRecyclerView.addOnItemTouchListener(
                new RecyclerView.SimpleOnItemTouchListener() {
                    @Override
                    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                        if (e.getAction() == MotionEvent.ACTION_UP) {
                            View childView = rv.findChildViewUnder(e.getX(), e.getY());
                            if (childView != null) {
                                int position = rv.getChildAdapterPosition(childView);
                                String[] diary = diaryList.get(position);
                                openeditdiarydialog(diary[0], diary[2], diary[1], diary[3], diary[4]);
                                return true;
                            }
                        }
                        return false;
                    }
                }
        );
    }


    private void showMoodDialog() {
        UserActivity.playButtonSound(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(DiaryActivity.this);
        builder.setTitle("Select Mood");

        String[] moodOptions = {"Happy","Calm", "Neutral", "Sad", "Angry"};
        builder.setItems(moodOptions, (dialog, which) -> {
            mood = moodOptions[which];
            diarymoodbutton.setText(mood);
            diarymoodbutton.setBackgroundColor(getColorForMood(mood));
        });

        builder.create().show();
    }

    private void showRatingDialog() {
        UserActivity.playButtonSound(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(DiaryActivity.this);
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
        Log.d("DEBUG", "diarytext value: " + textInTextView);

        if (textInTextView.equals("Type here...") || textInTextView.equals("Type here…")) {
            dialogEditText.setText("");
        }

        else {
            dialogEditText.setText(textInTextView);
        }
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button saveButton = dialogView.findViewById(R.id.save_button);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        saveButton.setOnClickListener(v -> {
            diarytext.setText(dialogEditText.getText().toString());
            diarytext.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.black)));
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveDiaryEntry() {
        String text = diarytext.getText().toString();

        if (text.isEmpty() || mood == null || rating == null || mood.equals("Mood") || rating.equals("Rating")) {
            Toast.makeText(DiaryActivity.this, "Please fill in all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (changedDate == null || changedTime == null) {
            changedDate = new SimpleDateFormat("d.M.yyyy", Locale.getDefault()).format(new Date());
            changedTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        }

        // Ensure the rating is stored as a string with the numeric value
        String ratingText = rating.replace("Rating: ", "");  // Remove the "Rating: " prefix
        if (firebaseuserboolean) {
            String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("diary_entries").child(userId);
            String entryId = databaseReference.push().getKey();

            if (entryId != null) {
                HashMap<String, Object> diaryData = new HashMap<>();
                diaryData.put("date", changedDate);
                diaryData.put("time", changedTime);
                diaryData.put("text", text);
                diaryData.put("mood", mood);
                diaryData.put("rating", ratingText);  // Store rating as string

                databaseReference.child(entryId).setValue(diaryData).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        UserActivity.playSaveSound(this);
                        Log.d("MainActivity", "Diary entry added to Firebase: " + entryId);
                        Toast.makeText(DiaryActivity.this, "Diary entry is saved to your Account.", Toast.LENGTH_SHORT).show();
                        resetForm();
                    } else {
                        Log.w("MainActivity", "Failed to add diary to Firebase", task.getException());
                        Toast.makeText(DiaryActivity.this, "There's been an error in saving.", Toast.LENGTH_SHORT).show();
                        firebaseuserboolean = false;
                        saveDiaryEntry();
                    }
                });
            }
        } else {
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_DATE_DIARY, changedDate);
            values.put(COLUMN_TIME_DIARY, changedTime);
            values.put(DatabaseHelper.COLUMN_TEXT_DIARY, text);
            values.put(DatabaseHelper.COLUMN_MOOD_DIARY, mood);
            values.put(DatabaseHelper.COLUMN_RATING_DIARY, ratingText);  // Store rating as string

            long newRowId = db.insert(DatabaseHelper.TABLE_DIARY, null, values);

            if (newRowId != -1) {
                UserActivity.playSaveSound(this);
                Log.d("MainActivity", "Diary entry saved to SQLite");
                Toast.makeText(DiaryActivity.this, "Diary entry is saved in your device", Toast.LENGTH_SHORT).show();
                resetForm();
            } else {
                Log.w("MainActivity", "Failed to save diary entry to SQLite.");
                Toast.makeText(DiaryActivity.this, "There's been an error in saving.", Toast.LENGTH_SHORT).show();
            }
        }
        recreate();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void resetForm() {
        diarytext.setText("Tap to edit diary text");
        diarymoodbutton.setText("Mood");
        diarymoodbutton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.second_button_color)));
        diaryratingbutton.setText("Rating");
        diaryratingbutton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.second_button_color)));
    }

    private void showMoodDialogBTN(Button moodButton) {
        UserActivity.playButtonSound(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(DiaryActivity.this);
        builder.setTitle("Select Mood");

        String[] moodOptions = {"Happy", "Calm", "Neutral", "Sad", "Angry"};
        builder.setItems(moodOptions, (dialog, which) -> {
            String selectedMood = moodOptions[which];
            moodButton.setText(selectedMood);
            moodButton.setBackgroundColor(getColorForMood(selectedMood));
        });

        builder.create().show();
    }

    private void showRatingDialogBTN(Button ratingButton) {
        UserActivity.playButtonSound(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(DiaryActivity.this);
        builder.setTitle("Rate Your Day");

        String[] ratingOptions = {"1", "2", "3", "4", "5"};
        builder.setItems(ratingOptions, (dialog, which) -> {
            String selectedRating = ratingOptions[which];
            ratingButton.setText("Rating: " + selectedRating); // Display formatted
            ratingButton.setBackgroundColor(getColorForRating(selectedRating));
        });

        builder.create().show();
    }

    private void openeditdiarydialog(String date, String time, String text, String mood, String rating) {
        UserActivity.playButtonSound(this);
        final Dialog dialog = new Dialog(DiaryActivity.this);
        dialog.setContentView(R.layout.dialog_edit_diary);

        EditText diaryText = dialog.findViewById(R.id.diaryText);
        EditText dateText = dialog.findViewById(R.id.dateText);
        EditText timeText = dialog.findViewById(R.id.timeText);
        Button diaryMoodButton = dialog.findViewById(R.id.diarymoodbutton);
        Button diaryRatingButton = dialog.findViewById(R.id.diaryratingbutton);

        diaryText.setText(text);
        dateText.setText(date);
        timeText.setText(time);
        diaryMoodButton.setText(mood);
        diaryRatingButton.setText(rating);

        diaryMoodButton.setBackgroundColor(getColorForMood(mood));
        diaryRatingButton.setBackgroundColor(getColorForRating(rating));

        dateText.setFocusableInTouchMode(true);
        dateText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                UserActivity.playButtonSound(this);
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(DiaryActivity.this,
                        (view, year1, month1, dayOfMonth) -> dateText.setText(dayOfMonth + "." + (month1 + 1) + "." + year1),
                        year, month, day);
                datePickerDialog.show();
            }
            return true;
        });

        timeText.setFocusableInTouchMode(true);
        timeText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                UserActivity.playButtonSound(this);
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(DiaryActivity.this,
                        (view, hourOfDay, minute1) -> timeText.setText(hourOfDay + ":" + (minute1 < 10 ? "0" + minute1 : minute1)),
                        hour, minute, true);
                timePickerDialog.show();
            }
            return true;
        });

        diaryMoodButton.setOnClickListener(v -> showMoodDialogBTN(diaryMoodButton));
        diaryRatingButton.setOnClickListener(v -> showRatingDialogBTN(diaryRatingButton));

        Button saveButton = dialog.findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            UserActivity.playSaveSound(this);
            String updatedText = diaryText.getText().toString();
            String updatedMood = diaryMoodButton.getText().toString();
            String ratingText = diaryRatingButton.getText().toString();
            String updatedRating = ratingText.replace("Rating: ", "");
            String updatedDate = dateText.getText().toString();
            String updatedTime = timeText.getText().toString();

            if (updatedText.isEmpty() || updatedMood.equals("Mood") || updatedRating.equals("Rating")) {
                Toast.makeText(DiaryActivity.this, "Please fill in all fields!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (firebaseuserboolean) {
                updatefirebasediary(date, time, updatedDate, updatedTime, updatedText, updatedMood, updatedRating);
            } else {
                updatesqldiary(date, time, updatedDate, updatedTime, updatedText, updatedMood, updatedRating);
            }
            dialog.dismiss();
        });

        Button cancelButton = dialog.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            UserActivity.playCancelSound(DiaryActivity.this);
            dialog.dismiss();
        });

        Button deleteButton = dialog.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(v -> {
            if (firebaseuserboolean) {
                deleteFirebaseEntry(date, time);
            } else {
                deleteSQLEntry(date, time);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updatefirebasediary(String olddate, String oldtime, String date, String time, String text, String mood, String rating) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("diary_entries").child(userId);

        databaseReference.orderByChild("date").equalTo(olddate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot entrySnapshot : snapshot.getChildren()) {
                    String entryTime = entrySnapshot.child("time").getValue(String.class);
                    if (entryTime != null && entryTime.equals(oldtime)) {
                        String entryKey = entrySnapshot.getKey();

                        if (entryKey != null) {
                            HashMap<String, Object> updatedData = new HashMap<>();
                            updatedData.put("date", date);
                            updatedData.put("time", time);
                            updatedData.put("text", text);
                            updatedData.put("mood", mood);
                            updatedData.put("rating", rating);  // Store rating as string

                            databaseReference.child(entryKey).updateChildren(updatedData)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(DiaryActivity.this, "Diary entry updated!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(DiaryActivity.this, "Failed to update entry", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DiaryActivity.this, "Error fetching data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatesqldiary(String olddate, String oldtime, String date, String time, String text, String mood, String rating) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        String selection = DatabaseHelper.COLUMN_DATE_DIARY + " = ? AND " + DatabaseHelper.COLUMN_TIME_DIARY + " = ?";
        String[] selectionArgs = { olddate, oldtime };

        Cursor cursor = db.query(DatabaseHelper.TABLE_DIARY, null, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_TEXT_DIARY, text);
            values.put(DatabaseHelper.COLUMN_MOOD_DIARY, mood);
            values.put(DatabaseHelper.COLUMN_RATING_DIARY, rating);
            values.put(DatabaseHelper.COLUMN_DATE_DIARY, date);
            values.put(DatabaseHelper.COLUMN_TIME_DIARY, time);

            int rowsAffected = db.update(DatabaseHelper.TABLE_DIARY, values, selection, selectionArgs);
            cursor.close();

            if (rowsAffected > 0) {
                Toast.makeText(DiaryActivity.this, "Diary entry updated!", Toast.LENGTH_SHORT).show();
                recreate();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } else {
                Toast.makeText(DiaryActivity.this, "Failed to update entry", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(DiaryActivity.this, "No matching diary entry found", Toast.LENGTH_SHORT).show();
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
    }

    private void deleteFirebaseEntry(String date, String time) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("diary_entries").child(userId);

        databaseReference.orderByChild("date").equalTo(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot entrySnapshot : snapshot.getChildren()) {
                    String entryTime = entrySnapshot.child("time").getValue(String.class);
                    if (entryTime != null && entryTime.equals(time)) {
                        String entryKey = entrySnapshot.getKey();

                        if (entryKey != null) {
                            databaseReference.child(entryKey).removeValue()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(DiaryActivity.this, "Diary entry deleted!", Toast.LENGTH_SHORT).show();
                                            recreate();
                                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                        } else {
                                            Toast.makeText(DiaryActivity.this, "Failed to delete entry", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DiaryActivity.this, "Error fetching data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteSQLEntry(String date, String time) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        String selection = DatabaseHelper.COLUMN_DATE_DIARY + " = ? AND " + DatabaseHelper.COLUMN_TIME_DIARY + " = ?";
        String[] selectionArgs = { date, time };

        int rowsDeleted = db.delete(DatabaseHelper.TABLE_DIARY, selection, selectionArgs);

        if (rowsDeleted > 0) {
            Toast.makeText(DiaryActivity.this, "Diary entry deleted!", Toast.LENGTH_SHORT).show();
            recreate();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            Toast.makeText(DiaryActivity.this, "Failed to delete entry", Toast.LENGTH_SHORT).show();
        }

        db.close();
    }
}