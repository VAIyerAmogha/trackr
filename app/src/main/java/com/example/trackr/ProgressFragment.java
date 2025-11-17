package com.example.trackr;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ProgressFragment extends Fragment {

    ListView listProgress;
    Button btnAddJournal;
    DBHelper dbHelper;
    ArrayList<String> habitList;
    ArrayList<Integer> habitIds;
    ArrayList<Boolean> habitDone;
    ArrayAdapter<String> adapter;
    String todayDate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_progress, container, false);

        listProgress = view.findViewById(R.id.listProgress);
        btnAddJournal = view.findViewById(R.id.btnAddJournal);
        dbHelper = new DBHelper(getContext());
        habitList = new ArrayList<>();
        habitIds = new ArrayList<>();
        habitDone = new ArrayList<>();

        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        loadTodayHabits();

        listProgress.setOnItemClickListener((adapterView, v, position, id) -> {
            if (!habitDone.get(position)) {
                dbHelper.markHabitCompleted(habitIds.get(position), todayDate);
                Toast.makeText(getContext(), "Marked as done!", Toast.LENGTH_SHORT).show();
                loadTodayHabits();
            } else {
                Toast.makeText(getContext(), "Already completed today!", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddJournal.setOnClickListener(v -> openJournalDialog());

        return view;
    }

    private void loadTodayHabits() {
        habitList.clear();
        habitIds.clear();
        habitDone.clear();

        Cursor cursor = dbHelper.getHabitsWithDateStatus(todayDate);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int doneToday = cursor.getInt(cursor.getColumnIndexOrThrow("done_today"));

                habitList.add(name + (doneToday == 1 ? " ✅" : " ⏺️"));
                habitIds.add(id);
                habitDone.add(doneToday == 1);
            } while (cursor.moveToNext());
        }
        cursor.close();

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, habitList);
        listProgress.setAdapter(adapter);

        // Check for 70% completion
        if (dbHelper.isDaySuccessful(todayDate)) {
            btnAddJournal.setVisibility(View.VISIBLE);
        } else {
            btnAddJournal.setVisibility(View.GONE);
        }
    }

    private void openJournalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Your Thoughts for Today");

        final EditText input = new EditText(getContext());
        input.setHint("Write your thoughts...");
        String existingEntry = dbHelper.getJournalEntry(todayDate);
        if (existingEntry != null) {
            input.setText(existingEntry);
        }
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String entry = input.getText().toString();
            if (!entry.isEmpty()) {
                dbHelper.addJournalEntry(todayDate, entry);
                Toast.makeText(getContext(), "Journal entry saved!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
