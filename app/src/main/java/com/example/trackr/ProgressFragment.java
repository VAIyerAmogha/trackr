package com.example.trackr;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ProgressFragment extends Fragment {

    ListView listProgress;
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
        dbHelper = new DBHelper(getContext());
        habitList = new ArrayList<>();
        habitIds = new ArrayList<>();
        habitDone = new ArrayList<>();

        // Get today's date
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Load today's habits
        loadTodayHabits();

        // When user taps a habit → mark completed
        listProgress.setOnItemClickListener((adapterView, v, position, id) -> {
            if (!habitDone.get(position)) {
                dbHelper.markHabitCompleted(habitIds.get(position), todayDate);
                Toast.makeText(getContext(), "Marked as done!", Toast.LENGTH_SHORT).show();
                loadTodayHabits();
            } else {
                Toast.makeText(getContext(), "Already completed today!", Toast.LENGTH_SHORT).show();
            }
        });

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
    }


}
