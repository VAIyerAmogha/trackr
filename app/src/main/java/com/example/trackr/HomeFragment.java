package com.example.trackr;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    EditText etHabitName;
    Spinner spinnerFrequency;
    TimePicker timePickerReminder;
    Button btnAddHabit;
    ListView listHabits;
    DBHelper dbHelper;
    ArrayList<String> habitsList;
    ArrayList<Integer> habitIds;
    ArrayAdapter<String> listAdapter;
    private ActivityResultLauncher<String> requestPermissionLauncher;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        addHabit();
                    } else {
                        Toast.makeText(getContext(), "Permission Denied. Reminders will not be created.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        etHabitName = view.findViewById(R.id.etHabitName);
        spinnerFrequency = view.findViewById(R.id.spinnerFrequency);
        timePickerReminder = view.findViewById(R.id.timePickerReminder);
        btnAddHabit = view.findViewById(R.id.btnAddHabit);
        listHabits = view.findViewById(R.id.listHabits);

        timePickerReminder.setIs24HourView(true);

        dbHelper = new DBHelper(getContext());
        habitsList = new ArrayList<>();
        habitIds = new ArrayList<>();

        String[] frequencies = {"Daily", "Weekly", "Monthly"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, frequencies);
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequency.setAdapter(freqAdapter);

        listAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, habitsList);
        listHabits.setAdapter(listAdapter);

        loadHabits();

        btnAddHabit.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    addHabit();
                }
            } else {
                addHabit();
            }
        });

        listHabits.setOnItemLongClickListener((adapterView, view1, i, l) -> {
            confirmDeleteHabit(habitIds.get(i));
            return true;
        });

        return view;
    }

    private void addHabit() {
        String name = etHabitName.getText().toString().trim();
        String frequency = spinnerFrequency.getSelectedItem().toString();
        int hour = timePickerReminder.getHour();
        int minute = timePickerReminder.getMinute();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a habit name", Toast.LENGTH_SHORT).show();
            return;
        }

        long result = dbHelper.addHabit(name, "🔥", frequency, hour, minute);

        if (result != -1) {
            ReminderUtils.scheduleHabitReminder(getContext(), name, hour, minute);
            Toast.makeText(getContext(), "Habit added with reminder!", Toast.LENGTH_SHORT).show();
            etHabitName.setText("");
            loadHabits();
        } else {
            Toast.makeText(getContext(), "Failed to add habit", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadHabits() {
        habitsList.clear();
        habitIds.clear();

        Cursor cursor = dbHelper.getAllHabits();
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String freq = cursor.getString(cursor.getColumnIndexOrThrow("frequency"));
                habitsList.add(name + " (" + freq + ")");
                habitIds.add(id);
            } while (cursor.moveToNext());
        }
        cursor.close();

        listAdapter.notifyDataSetChanged();
    }

    private void confirmDeleteHabit(int habitId) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Habit")
                .setMessage("Are you sure you want to delete this habit?")
                .setPositiveButton("Yes", (dialog, which) -> deleteHabit(habitId))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteHabit(int habitId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("habits", "id=?", new String[]{String.valueOf(habitId)});
        db.close();
        Toast.makeText(getContext(), "Habit deleted", Toast.LENGTH_SHORT).show();
        loadHabits();
    }
}
