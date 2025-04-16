package com.veroronika.studyplanner.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "studyPlanner.db";
    private static final int DATABASE_VERSION = 13;

    public static final String TABLE_DIARY = "Diary";
    public static final String COLUMN_DATE_DIARY = "date";
    public static final String COLUMN_TEXT_DIARY = "text";
    public static final String COLUMN_MOOD_DIARY = "mood";
    public static final String COLUMN_RATING_DIARY = "rating";
    public static final String COLUMN_TIME_DIARY = "time";

    public static final String TABLE_STUDY = "Study";
    public static final String COLUMN_DATE_REMINDER = "date_reminder";
    public static final String COLUMN_TEXT_REMINDER = "text_reminder";
    public static final String COLUMN_TAG_INDEX = "tag_index";
    public static final String COLUMN_SUBJECT_NAME = "subject_name";
    public static final String COLUMN_TIME_REMINDER = "timeReminder";

    public static final String TABLE_STUDY_SESSION = "StudySession";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_SUBJECT_NAME_SESSION = "subject_name";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_TAG_INDEX_SESSION = "tag_index";
    public static final String COLUMN_STOPWATCH_TIME = "stopwatch_time";

    public static final String TABLE_SUBJECT_NAMES = "SubjectNames";
    public static final String COLUMN_SUBJECT_NAME_TAG = "subject_name";
    public static final String COLUMN_TAG_INDEX_SUBJECT = "tag_index";

    public static final String TABLE_MONTHLY_GOALS = "MonthlyGoals";
    public static final String COLUMN_MONTH_YEAR = "month_year";
    public static final String COLUMN_SUBJECT_NAME_GOAL = "subject_name";
    public static final String COLUMN_GOAL_TIME = "goal_time";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createDiaryTable = "CREATE TABLE " + TABLE_DIARY + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DATE_DIARY + " TEXT NOT NULL, " +
                COLUMN_TIME_DIARY + " TEXT," +
                COLUMN_TEXT_DIARY + " TEXT, " +
                COLUMN_MOOD_DIARY + " TEXT, " +
                COLUMN_RATING_DIARY + " TEXT)";
        db.execSQL(createDiaryTable);

        String createStudyTable = "CREATE TABLE " + TABLE_STUDY + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DATE_REMINDER + " TEXT NOT NULL, " +
                COLUMN_TEXT_REMINDER + " TEXT, " +
                COLUMN_TAG_INDEX + " INTEGER, " +
                COLUMN_SUBJECT_NAME + " TEXT, " +
                COLUMN_TIME_REMINDER + " TEXT" +
                ");";
        db.execSQL(createStudyTable);

        String createStudySessionTable = "CREATE TABLE " + TABLE_STUDY_SESSION + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DATE + " TEXT NOT NULL, " +
                COLUMN_TIME + " TEXT, " +
                COLUMN_SUBJECT_NAME_SESSION + " TEXT, " +
                COLUMN_TEXT + " TEXT, " +
                COLUMN_TAG_INDEX_SESSION + " INTEGER, " +
                COLUMN_STOPWATCH_TIME + " TEXT" +
                ");";
        db.execSQL(createStudySessionTable);

        String createSubjectNamesTable = "CREATE TABLE " + TABLE_SUBJECT_NAMES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SUBJECT_NAME_TAG + " TEXT NOT NULL, " +
                COLUMN_TAG_INDEX_SUBJECT + " INTEGER" +
                ");";
        db.execSQL(createSubjectNamesTable);

        String createMonthlyGoalsTable = "CREATE TABLE " + TABLE_MONTHLY_GOALS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_MONTH_YEAR + " TEXT NOT NULL, " +
                COLUMN_SUBJECT_NAME_GOAL + " TEXT NOT NULL, " +
                COLUMN_GOAL_TIME + " INTEGER NOT NULL" +
                ");";
        db.execSQL(createMonthlyGoalsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 13) {
            String createMonthlyGoalsTable = "CREATE TABLE " + TABLE_MONTHLY_GOALS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_MONTH_YEAR + " TEXT NOT NULL, " +
                    COLUMN_SUBJECT_NAME_GOAL + " TEXT NOT NULL, " +
                    COLUMN_GOAL_TIME + " INTEGER NOT NULL" +
                    ");";
            db.execSQL(createMonthlyGoalsTable);
        }
    }
}
