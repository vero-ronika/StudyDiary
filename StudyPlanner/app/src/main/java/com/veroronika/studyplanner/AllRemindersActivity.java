package com.veroronika.studyplanner;

import static com.veroronika.studyplanner.StudyActivity.capitalizeWords;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Collections;


public class AllRemindersActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private RecyclerView remindersRecyclerView;
    private RemindersAdapter remindersAdapter;
    private List<String[]> reminderList;
    public boolean firebaseuserboolean;
    String userId;
    FirebaseAuth mAuth;

    private int tagIndex = -1;
    private String selectedDate;
    private String reminderTime;

    BottomNavigationView bottomNavigationView;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        databaseHelper = new DatabaseHelper(this);

        mAuth = FirebaseAuth.getInstance();

        createNotificationChannel(this);

        setContentView(R.layout.activity_all_reminders);

        remindersRecyclerView = findViewById(R.id.remindersRecyclerView);
        if (remindersRecyclerView == null) {
            Log.e("AllRemindersActivity", "RecyclerView not found!");
        }

        bottomNavigationView = findViewById(R.id.bottomnavigationview);
        int selectedItemId = R.id.navigation_study;

        bottomNavigationView.setSelectedItemId(selectedItemId);
        ColorStateList colorStateList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.main_default_dark)
        );
        bottomNavigationView.setItemActiveIndicatorColor(colorStateList);

        remindersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

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
                loadRemindersFromFirebase();
            } else {
                firebaseuserboolean = false;
                loadRemindersFromSQL();
            }
        } else {
            firebaseuserboolean = false;
            loadRemindersFromSQL();
        }
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent;
            switch (Objects.requireNonNull(item.getTitle()).toString()) {
                case "Home":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(AllRemindersActivity.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;

                case "Study":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(AllRemindersActivity.this, StudyActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;

                case "Diary":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(AllRemindersActivity.this, DiaryActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;

                case "User":
                    UserActivity.playButtonSound(this);
                    intent = new Intent(AllRemindersActivity.this, UserActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                default:
                    return false;
            }
        });
    }

    private void loadRemindersFromSQL() {
        reminderList = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_STUDY,
                null,
                null,
                null,
                null,
                null,
                DatabaseHelper.COLUMN_DATE_REMINDER + " DESC, " + DatabaseHelper.COLUMN_TIME_REMINDER + " DESC"
        );
        if (cursor != null && cursor.moveToFirst()) {
            int dateColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE_REMINDER);
            int textColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT_REMINDER);
            int timeColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME_REMINDER);
            int subjectColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SUBJECT_NAME);
            int tagColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TAG_INDEX);

            if (dateColumnIndex >= 0 && textColumnIndex >= 0 && timeColumnIndex >= 0 && subjectColumnIndex >= 0 && tagColumnIndex >= 0) {
                do {
                    String dateReminder = cursor.getString(dateColumnIndex);
                    String textReminder = cursor.getString(textColumnIndex);
                    String timeReminder = cursor.getString(timeColumnIndex);
                    String subjectName = cursor.getString(subjectColumnIndex);
                    int tagIndex = cursor.getInt(tagColumnIndex);

                    reminderList.add(new String[]{dateReminder, textReminder, timeReminder, subjectName, String.valueOf(tagIndex)});
                } while (cursor.moveToNext());
            } else {
                Log.e("AllRemindersActivity", "Column index is invalid");
            }
        }
        assert cursor != null;
        cursor.close();

        updateRecyclerView();
    }


    private void loadRemindersFromFirebase() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("study_entries").child(userId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                reminderList = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    List<DataSnapshot> snapshotList = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        snapshotList.add(snapshot);
                    }

                    Collections.reverse(snapshotList);
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String dateReminder = snapshot.child("dateReminder").getValue(String.class);
                        String textReminder = snapshot.child("textReminder").getValue(String.class);
                        String timeReminder = snapshot.child("timeReminder").getValue(String.class);
                        String subjectName = snapshot.child("subjectName").getValue(String.class);
                        int tagIndex = snapshot.child("tagIndex").getValue(Integer.class);

                        Log.d("FirebaseData", "Date: " + dateReminder + ", Text: " + textReminder + ", Time: " + timeReminder + ", Subject: " + subjectName + ", TagIndex: " + tagIndex);

                        reminderList.add(new String[]{dateReminder, textReminder, timeReminder, subjectName, String.valueOf(tagIndex)});
                    }
                } else {
                    Log.w("FirebaseData", "No data found for userId: " + userId);
                }
                updateRecyclerView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("AllRemindersActivity", "loadRemindersEntries:onCancelled", databaseError.toException());
            }
        });
    }


    private void updateRecyclerView() {
        if (remindersAdapter == null) {
            remindersAdapter = new RemindersAdapter(reminderList, tags);

        } else {
            remindersAdapter.notifyDataSetChanged();
        }
        remindersRecyclerView.setAdapter(remindersAdapter);
    }


    private boolean isConnectedToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
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

    public void loadFullReminderDetails(int reminderIndex) {
        if (firebaseuserboolean) {
            loadReminderDetailsFromFirebase(reminderIndex);
        } else {
            loadReminderDetailsFromSQL(reminderIndex);
        }
    }

    private void loadReminderDetailsFromFirebase(int reminderIndex) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("study_entries").child(userId);

        database.orderByChild("tagIndex").equalTo(reminderIndex).limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                String dateReminder = snapshot.child("dateReminder").getValue(String.class);
                                String textReminder = snapshot.child("textReminder").getValue(String.class);
                                String timeReminder = snapshot.child("timeReminder").getValue(String.class);
                                String subjectName = snapshot.child("subjectName").getValue(String.class);

                                openEditReminderDialog(dateReminder, timeReminder, textReminder, subjectName, reminderIndex);
                            }
                        } else {
                            Toast.makeText(AllRemindersActivity.this, "No reminder found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w("Firebase", "Failed to load reminder details", databaseError.toException());
                    }
                });
    }

    private void loadReminderDetailsFromSQL(int reminderIndex) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String selection = DatabaseHelper.COLUMN_TAG_INDEX + " = ?";
        String[] selectionArgs = {String.valueOf(reminderIndex)};

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

            String dateReminder = dateReminderIndex != -1 ? cursor.getString(dateReminderIndex) : "";
            String textReminder = textReminderIndex != -1 ? cursor.getString(textReminderIndex) : "";
            String timeReminder = timeReminderIndex != -1 ? cursor.getString(timeReminderIndex) : "";
            String subjectName = subjectNameIndex != -1 ? cursor.getString(subjectNameIndex) : "";

            openEditReminderDialog(dateReminder, timeReminder, textReminder, subjectName, reminderIndex);
        } else {
            Toast.makeText(this, "No reminder found", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
    }

    private int selectedTagIndex = -1;

    public void setTagSelectionBySubject(Dialog dialog, int tagIndex, String subjectName, TextView tag_selection_name, EditText subjectEditText) {
        if (tagIndex >= 0 && tagIndex < tags.length) { 
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
        } else {
            Log.e("TAG_ERROR", "Invalid tagIndex: " + tagIndex + ", tags.length: " + tags.length);
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
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("subjectnames");
                Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

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

                DatePickerDialog datePickerDialog = new DatePickerDialog(AllRemindersActivity.this,
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

                TimePickerDialog timePickerDialog = new TimePickerDialog(AllRemindersActivity.this,
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

                                Toast.makeText(AllRemindersActivity.this, "Reminder updated in Firebase", Toast.LENGTH_SHORT).show();
                                loadRemindersFromFirebase();
                                break;
                            }
                        }

                        if (!reminderFound) {
                            Toast.makeText(AllRemindersActivity.this, "No reminder found for the given date and time", Toast.LENGTH_SHORT).show();
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
            ContentValues subjectValues = new ContentValues();
            subjectValues.put(DatabaseHelper.COLUMN_SUBJECT_NAME, newSubject);

            Cursor cursor = db.query(DatabaseHelper.TABLE_SUBJECT_NAMES, null,
                    DatabaseHelper.COLUMN_SUBJECT_NAME + " = ?", new String[]{newSubject},
                    null, null, null);

            if (cursor.getCount() == 0) {
                db.insert(DatabaseHelper.TABLE_SUBJECT_NAMES, null, subjectValues);
            }
            cursor.close();

            Toast.makeText(this, "Reminder updated in SQL database", Toast.LENGTH_SHORT).show();
            loadRemindersFromSQL();
        } else {
            Toast.makeText(this, "Failed to update reminder in SQL database", Toast.LENGTH_SHORT).show();
        }
    }
}
