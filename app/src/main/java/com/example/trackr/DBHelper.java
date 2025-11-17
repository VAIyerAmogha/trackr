package com.example.trackr;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "trackr.db";
    private static final int DB_VERSION = 3; // Incremented DB version

    // Table names
    private static final String TABLE_HABITS = "habits";
    private static final String TABLE_PROGRESS = "habit_progress";
    private static final String TABLE_STREAKS = "streaks";
    private static final String TABLE_JOURNALS = "journals"; // New journals table

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createHabits = "CREATE TABLE " + TABLE_HABITS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "icon TEXT, " +
                "frequency TEXT, " +
                "reminder_hour INTEGER, " +
                "reminder_minute INTEGER)";

        String createProgress = "CREATE TABLE " + TABLE_PROGRESS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "habit_id INTEGER, " +
                "date TEXT, " +
                "completed INTEGER, " +
                "UNIQUE(habit_id, date))";

        String createStreaks = "CREATE TABLE " + TABLE_STREAKS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "start_date TEXT, " +
                "current_streak INTEGER, " +
                "longest_streak INTEGER)";

        String createJournals = "CREATE TABLE " + TABLE_JOURNALS + " (" +
                "date TEXT PRIMARY KEY, " +
                "entry TEXT)";

        db.execSQL(createHabits);
        db.execSQL(createProgress);
        db.execSQL(createStreaks);
        db.execSQL(createJournals);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_HABITS + " ADD COLUMN reminder_hour INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_HABITS + " ADD COLUMN reminder_minute INTEGER");
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE " + TABLE_JOURNALS + " (" +
                    "date TEXT PRIMARY KEY, " +
                    "entry TEXT)");
        }
    }


    public long addHabit(String name, String icon, String frequency, int hour, int minute) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("icon", icon);
        cv.put("frequency", frequency);
        cv.put("reminder_hour", hour);
        cv.put("reminder_minute", minute);
        long id = db.insert(TABLE_HABITS, null, cv);
        db.close();
        return id;
    }

    public Cursor getAllHabits() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_HABITS, null);
    }

    public void markHabitCompleted(int habitId, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("habit_id", habitId);
        values.put("date", date);
        values.put("completed", 1);
        db.insertWithOnConflict(TABLE_PROGRESS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public boolean isHabitCompleted(int habitId, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PROGRESS + " WHERE habit_id=? AND date=?",
                new String[]{String.valueOf(habitId), date});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }


    public Cursor getHabitsWithDateStatus(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT h.id, h.name, h.icon, " +
                "CASE WHEN p.completed IS NOT NULL THEN 1 ELSE 0 END AS done_today " +
                "FROM " + TABLE_HABITS + " h " +
                "LEFT JOIN " + TABLE_PROGRESS + " p ON h.id = p.habit_id AND p.date = ?";
        return db.rawQuery(query, new String[]{date});
    }

    // --- STREAK SYSTEM METHODS ---

    // Get the most recent date with progress
    public String getLastProgressDate() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT date FROM habit_progress ORDER BY date DESC LIMIT 1", null);
        String date = null;
        if (cursor.moveToFirst()) {
            date = cursor.getString(0);
        }
        cursor.close();
        return date;
    }

    // Check if all habits were completed on a given date
    public boolean isDaySuccessful(String date) {
        Cursor allHabits = getAllHabits();
        int total = allHabits.getCount();
        allHabits.close();

        if (total == 0) return false;

        Cursor done = getHabitsWithDateStatus(date);
        int completed = 0;
        if (done.moveToFirst()) {
            do {
                int doneToday = done.getInt(done.getColumnIndexOrThrow("done_today"));
                if (doneToday == 1) completed++;
            } while (done.moveToNext());
        }
        done.close();

        float percent = (completed * 100f / total);
        return percent >= 70f; // Streak continues if ≥70%
    }

    // Save or update current streak
    public void updateStreak(boolean successful) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT current_streak, longest_streak FROM streaks WHERE id = 1", null);

        int current = 0;
        int longest = 0;

        if (cursor.moveToFirst()) {
            current = cursor.getInt(0);
            longest = cursor.getInt(1);
        }
        cursor.close();

        if (successful) current++; else current = 0;
        if (current > longest) longest = current;

        ContentValues cv = new ContentValues();
        cv.put("id", 1);
        cv.put("current_streak", current);
        cv.put("longest_streak", longest);
        cv.put("start_date", getCurrentDate());

        if (streakExists()) {
            db.update("streaks", cv, "id = 1", null);
        } else {
            db.insert("streaks", null, cv);
        }
        db.close();
    }

    private boolean streakExists() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM streaks WHERE id = 1", null);
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    public Cursor getStreakInfo() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM streaks WHERE id = 1", null);
    }

    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    // --- JOURNAL METHODS ---

    public void addJournalEntry(String date, String entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("date", date);
        cv.put("entry", entry);
        db.insertWithOnConflict(TABLE_JOURNALS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public String getJournalEntry(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT entry FROM " + TABLE_JOURNALS + " WHERE date=?", new String[]{date});
        String entry = null;
        if (cursor.moveToFirst()) {
            entry = cursor.getString(cursor.getColumnIndexOrThrow("entry"));
        }
        cursor.close();
        db.close();
        return entry;
    }
}
