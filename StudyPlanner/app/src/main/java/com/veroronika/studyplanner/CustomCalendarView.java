package com.veroronika.studyplanner;

import static com.veroronika.studyplanner.StudyActivity.capitalizeWords;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.veroronika.studyplanner.database.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;

public class CustomCalendarView extends LinearLayout implements MainActivity.FirebaseUserStatusListener {
    private boolean firebaseuserboolean;
    private DatabaseHelper databaseHelper;

    private TextView monthYearDisplay;
    private GridLayout calendarGrid;
    private int currentMonth, currentYear;
    private SimpleDateFormat dateFormat;

    private int tagIndex = -1;
    private String selectedDate;
    private String reminderTime;

    List<Pair<Long, Pair<Integer, String>>> reminderDetails = new ArrayList<>();

    List<Pair<String, Pair<Long, Pair<String, Pair<String, String>>>>> diaryDetails = new ArrayList<>();

    final int[] tags = {
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

    @Override
    public void onFirebaseUserStatusUpdated(boolean firebaseUserStatus) {
        Log.d("CustomCalendarView", "firebase user status updated: " + firebaseUserStatus);
        firebaseuserboolean = firebaseUserStatus;
    }

    public CustomCalendarView(Context context) {
        super(context);
        init(context);
    }

    public CustomCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        Log.d("CustomCalendarView", "Initializing CustomCalendarView");
        reminderDetails = new ArrayList<>();
        dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        LayoutInflater.from(context).inflate(R.layout.custom_calendar_view, this, true);
        databaseHelper = new DatabaseHelper(context);

        monthYearDisplay = findViewById(R.id.month_year_display);
        calendarGrid = findViewById(R.id.calendar_grid);


        Calendar calendar = Calendar.getInstance();
        currentMonth = calendar.get(Calendar.MONTH);
        currentYear = calendar.get(Calendar.YEAR);


        updateMonthYearDisplay();


        findViewById(R.id.prev_month_button).setOnClickListener(v -> changeMonth(-1));
        findViewById(R.id.next_month_button).setOnClickListener(v -> changeMonth(1));


        populateCalendar();

        MainActivity mainActivity = new MainActivity();
        firebaseuserboolean = mainActivity.getFirebaseUserBoolean();
        Log.d("CustomCalendarView", "firebase user" + firebaseuserboolean);

    }

    private void updateMonthYearDisplay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(currentYear, currentMonth, 1);
        String monthYearText = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.getTime());
        monthYearDisplay.setText(monthYearText);
    }

    private void changeMonth(int delta) {
        currentMonth += delta;

        if (currentMonth < 0) {
            currentMonth = 11;
            currentYear--;
        } else if (currentMonth > 11) {
            currentMonth = 0;
            currentYear++;
        }

        updateMonthYearDisplay();
        populateCalendar();
    }

    private void populateCalendar() {
        calendarGrid.removeAllViews();

        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String dayName : dayNames) {
            LinearLayout dayNameContainer = getLinearLayout(dayName);
            calendarGrid.addView(dayNameContainer);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(currentYear, currentMonth, 1);

        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 1; i < firstDayOfWeek; i++) {
            TextView emptyCell = new TextView(getContext());
            emptyCell.setVisibility(GONE);
            calendarGrid.addView(emptyCell);
        }

        for (int day = 1; day <= daysInMonth; day++) {

            FrameLayout dayContainer = new FrameLayout(getContext());
            dayContainer.setPadding(5, 5, 5, 5);

            TextView dateView = new TextView(getContext());
            dateView.setText(String.valueOf(day));
            dateView.setGravity(Gravity.CENTER);
            dateView.setTextColor(Color.BLACK);
            dateView.setPadding(10, 10, 10, 10);

            FrameLayout.LayoutParams dateParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            dateParams.gravity = Gravity.CENTER;
            dateView.setLayoutParams(dateParams);

            LinearLayout tagsContainer = new LinearLayout(getContext());
            tagsContainer.setOrientation(LinearLayout.HORIZONTAL);
            tagsContainer.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams tagsParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            tagsParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            tagsParams.topMargin = 15;
            tagsContainer.setLayoutParams(tagsParams);
            tagsContainer.setMinimumWidth(100);
            dayContainer.addView(dateView);
            dayContainer.addView(tagsContainer);

            long dateMillis = getDateInMillis(currentYear, currentMonth, day);
            List<Integer> dayTags = new ArrayList<>();

            for (Pair<Long, Pair<Integer, String>> reminder : reminderDetails) {
                String reminderDateStr = dateFormat.format(new Date(reminder.first));
                String currentDateStr = dateFormat.format(new Date(dateMillis));
                if (reminderDateStr.equals(currentDateStr)) {
                    int tagIndex = reminder.second.first;
                    if (tagIndex >= 0 && tagIndex < tags.length) {
                        dayTags.add(tagIndex);
                    }
                    dateView.setOnClickListener(v -> loadReminderDetails(reminderDateStr, reminder.second.second));
                }
            }
            for (int i = 0; i < Math.min(3, dayTags.size()); i++) {
                View tagView = new View(getContext());
                LinearLayout.LayoutParams tagLayoutParams = new LinearLayout.LayoutParams(20, 20);
                tagLayoutParams.setMargins(5, 0, 5, 0);
                tagView.setLayoutParams(tagLayoutParams);
                tagView.setBackgroundResource(tags[dayTags.get(i)]);
                tagsContainer.addView(tagView);
            }
            calendarGrid.addView(dayContainer);
        }



    }

    private @NonNull LinearLayout getLinearLayout(String dayName) {
        LinearLayout dayNameContainer = new LinearLayout(getContext());
        dayNameContainer.setOrientation(LinearLayout.HORIZONTAL);
        dayNameContainer.setGravity(Gravity.CENTER);
        dayNameContainer.setMinimumWidth(100);
        dayNameContainer.setPadding(5, 5, 5, 5);

        TextView dayNameView = new TextView(getContext());
        dayNameView.setText(dayName);
        dayNameView.setGravity(Gravity.CENTER);
        dayNameView.setTextColor(Color.BLACK);
        dayNameView.setPadding(10, 10, 10, 10);

        dayNameContainer.addView(dayNameView);
        return dayNameContainer;
    }

    public void setReminderDates(List<Pair<Long, Pair<Integer, String>>> reminderDetails) {
        this.reminderDetails = reminderDetails;
        populateCalendar();
    }

    private long getDateInMillis(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth, 0, 0, 0);
        return calendar.getTimeInMillis();
    }

    public void setDiaryDates(List<Pair<String, Pair<Long, Pair<String, Pair<String, String>>>>> diaryDetails) {
        this.diaryDetails = diaryDetails;
        populateCalendarDiary();
    }

    private void populateCalendarDiary() {
        calendarGrid.removeAllViews();

        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String dayName : dayNames) {
            LinearLayout dayNameContainer = getLinearLayout(dayName);
            calendarGrid.addView(dayNameContainer);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(currentYear, currentMonth, 1);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);


        for (int i = 1; i < firstDayOfWeek; i++) {
            TextView emptyCell = new TextView(getContext());
            emptyCell.setVisibility(GONE);
            calendarGrid.addView(emptyCell);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            FrameLayout dayContainer = new FrameLayout(getContext());
            dayContainer.setPadding(5, 5, 5, 5);


            TextView dateView = new TextView(getContext());
            dateView.setText(String.valueOf(day));
            dateView.setGravity(Gravity.CENTER);
            dateView.setTextColor(Color.BLACK);
            dateView.setPadding(10, 10, 10, 10);

            FrameLayout.LayoutParams dateParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            dateParams.gravity = Gravity.CENTER;
            dateView.setLayoutParams(dateParams);


            LinearLayout tagsContainer = new LinearLayout(getContext());
            tagsContainer.setOrientation(LinearLayout.HORIZONTAL);
            tagsContainer.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams tagsParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            tagsParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            tagsParams.topMargin = 15;
            tagsContainer.setLayoutParams(tagsParams);
            tagsContainer.setMinimumWidth(100);

            dayContainer.addView(dateView);
            dayContainer.addView(tagsContainer);


            long dateMillis = getDateInMillis(currentYear, currentMonth, day);


            Log.d("CustomCalendarView", "Checking date: " + dateMillis);


            boolean hasEntry = false;
            for (Pair<String, Pair<Long, Pair<String, Pair<String, String>>>> diary : diaryDetails) {
                long diaryDateMillis = diary.second.first;

                if (isSameDay(diaryDateMillis, dateMillis)) {
                    hasEntry = true;

                    String datestr = dateFormat.format(new Date(diary.second.first));

                    String ratingDiary = diary.first;
                    String moodDiary = diary.second.second.first;
                    String timeDiary = diary.second.second.second.first;
                    String textDiary = diary.second.second.second.second;

                    int moodColor = getColorForMood(moodDiary);
                    int ratingColor = getColorForRating(ratingDiary);

                    View moodCircle = new View(getContext());
                    LayoutParams moodLayoutParams = new LayoutParams(20, 20);
                    moodLayoutParams.setMargins(5, 0, 5, 0);
                    moodCircle.setLayoutParams(moodLayoutParams);
                    moodCircle.setBackground(createCircleDrawable(moodColor));

                    View ratingCircle = new View(getContext());
                    LayoutParams ratingLayoutParams = new LayoutParams(20, 20);
                    ratingLayoutParams.setMargins(5, 0, 5, 0);
                    ratingCircle.setLayoutParams(ratingLayoutParams);
                    ratingCircle.setBackground(createCircleDrawable(ratingColor));

                    tagsContainer.addView(moodCircle);
                    tagsContainer.addView(ratingCircle);

                    dateView.setOnClickListener(v -> openeditdiarydialog(datestr,timeDiary,textDiary,moodDiary,ratingDiary));

                    break;
                }
            }

            if (hasEntry) {
                Log.d("CustomCalendarView", "Diary entry found for date: " + dateMillis);
            }

            calendarGrid.addView(dayContainer);
        }
    }


    private boolean isSameDay(long date1, long date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(date1);
        cal1.set(Calendar.HOUR_OF_DAY, 0);
        cal1.set(Calendar.MINUTE, 0);
        cal1.set(Calendar.SECOND, 0);
        cal1.set(Calendar.MILLISECOND, 0);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(date2);
        cal2.set(Calendar.HOUR_OF_DAY, 0);
        cal2.set(Calendar.MINUTE, 0);
        cal2.set(Calendar.SECOND, 0);
        cal2.set(Calendar.MILLISECOND, 0);

        return cal1.getTimeInMillis() == cal2.getTimeInMillis();
    }



    private Drawable createCircleDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setSize(20, 20);
        drawable.setColor(color);
        return drawable;
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



    public void loadReminderDetails(String date, String time) {
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
                            Toast.makeText(CustomCalendarView.this.getContext(), "No reminder found", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(CustomCalendarView.this.getContext(), "No reminder found", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
    }

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
        UserActivity.playButtonSound(CustomCalendarView.this.getContext());
        final Dialog dialog = new Dialog(CustomCalendarView.this.getContext());
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
            int tagId = getResources().getIdentifier("tag" + i, "id", getContext().getPackageName());
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
            UserActivity.playSaveSound(CustomCalendarView.this.getContext());
            String updatedDate = dateEditText.getText().toString().trim();
            String updatedText = textEditText.getText().toString().trim();
            String updatedTime = timeEditText.getText().toString().trim();
            String updatedSubject = subjectEditText.getText().toString().trim();

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
                UserActivity.playButtonSound(CustomCalendarView.this.getContext());
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(CustomCalendarView.this.getContext(),
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
                UserActivity.playButtonSound(CustomCalendarView.this.getContext());
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(CustomCalendarView.this.getContext(),
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
            UserActivity.playCancelSound(CustomCalendarView.this.getContext());
            dialog.dismiss();
        });

        dialog.show();
    }

    private int selectedTagIndex = -1;

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

                                Toast.makeText(CustomCalendarView.this.getContext(), "Reminder updated in Firebase", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }

                        if (!reminderFound) {
                            Toast.makeText(CustomCalendarView.this.getContext(), "No reminder found for the given date and time", Toast.LENGTH_SHORT).show();
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

            Toast.makeText(CustomCalendarView.this.getContext(), "Reminder updated in SQL database", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(CustomCalendarView.this.getContext(), "Failed to update reminder in SQL database", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMoodDialogBTN(Button moodButton) {
        UserActivity.playButtonSound(CustomCalendarView.this.getContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(CustomCalendarView.this.getContext());
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
        UserActivity.playButtonSound(CustomCalendarView.this.getContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(CustomCalendarView.this.getContext());
        builder.setTitle("Rate Your Day");

        String[] ratingOptions = {"1", "2", "3", "4", "5"};
        builder.setItems(ratingOptions, (dialog, which) -> {
            String selectedRating = ratingOptions[which];
            ratingButton.setText("Rating: " + selectedRating);
            ratingButton.setBackgroundColor(getColorForRating(selectedRating));
        });

        builder.create().show();
    }

    private void openeditdiarydialog(String date, String time, String text, String mood, String rating) {
        UserActivity.playButtonSound(CustomCalendarView.this.getContext());
        final Dialog dialog = new Dialog(CustomCalendarView.this.getContext());
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
                UserActivity.playButtonSound(CustomCalendarView.this.getContext());
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(CustomCalendarView.this.getContext(),
                        (view, year1, month1, dayOfMonth) -> dateText.setText(dayOfMonth + "." + (month1 + 1) + "." + year1),
                        year, month, day);
                datePickerDialog.show();
            }
            return true;
        });

        timeText.setFocusableInTouchMode(true);
        timeText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                UserActivity.playButtonSound(CustomCalendarView.this.getContext());
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(CustomCalendarView.this.getContext(),
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
            UserActivity.playSaveSound(CustomCalendarView.this.getContext());
            String updatedText = diaryText.getText().toString();
            String updatedMood = diaryMoodButton.getText().toString();
            String updatedRating = diaryRatingButton.getText().toString();
            String updatedDate = dateText.getText().toString();
            String updatedTime = timeText.getText().toString();

            if (updatedText.isEmpty() || updatedMood.equals("Mood") || updatedRating.equals("Rating")) {
                Toast.makeText(CustomCalendarView.this.getContext(), "Please fill in all fields!", Toast.LENGTH_SHORT).show();
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
            UserActivity.playCancelSound(CustomCalendarView.this.getContext());
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
                                            Toast.makeText(CustomCalendarView.this.getContext(), "Diary entry deleted!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(CustomCalendarView.this.getContext(), "Failed to delete entry", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomCalendarView.this.getContext(), "Error fetching data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteSQLEntry(String date, String time) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        String selection = DatabaseHelper.COLUMN_DATE_DIARY + " = ? AND " + DatabaseHelper.COLUMN_TIME_DIARY + " = ?";
        String[] selectionArgs = { date, time };

        int rowsDeleted = db.delete(DatabaseHelper.TABLE_DIARY, selection, selectionArgs);

        if (rowsDeleted > 0) {
            Toast.makeText(CustomCalendarView.this.getContext(), "Diary entry deleted!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(CustomCalendarView.this.getContext(), "Failed to delete entry", Toast.LENGTH_SHORT).show();
        }

        db.close();
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
                            updatedData.put("rating", rating);

                            databaseReference.child(entryKey).updateChildren(updatedData)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(CustomCalendarView.this.getContext(), "Diary entry updated!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(CustomCalendarView.this.getContext(), "Failed to update entry", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomCalendarView.this.getContext(), "Error fetching data", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(CustomCalendarView.this.getContext(), "Diary entry updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(CustomCalendarView.this.getContext(), "Failed to update entry", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(CustomCalendarView.this.getContext(), "No matching diary entry found", Toast.LENGTH_SHORT).show();
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
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

    private void getSubjectNameFromTagFirebase(int tagIndex, final StudyActivity.SubjectNameCallback callback) {
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

}
