package com.example.trackr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    CalendarView calendarView;
    TextView textStatus, textMonthSummary;
    DBHelper dbHelper;
    String selectedDate = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        textStatus = view.findViewById(R.id.textSelectedStatus);
        textMonthSummary = view.findViewById(R.id.textMonthSummary);

        dbHelper = new DBHelper(getContext());

        calendarView.setOnDateChangeListener((view1, year, month, day) -> {
            selectedDate = year + "-" + format(month + 1) + "-" + format(day);
            showStatus();
        });

        updateMonthlyStats();
        return view;
    }

    private void showStatus() {
        boolean marked = dbHelper.isDayMarked(selectedDate);
        if (marked) {
            textStatus.setText("❌ Marked complete for: " + selectedDate);
        } else {
            textStatus.setText("Not completed: " + selectedDate);
        }
    }

    private void updateMonthlyStats() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int success = 0, failed = 0, noData = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(cal.getTime());

        for (int day = 1; day <= daysInMonth; day++) {
            String date = year + "-" + format(month) + "-" + format(day);

            if (dbHelper.isDayMarked(date)) {
                success++;
            } else if (date.compareTo(today) < 0) { // date has passed
                failed++;
            } else {
                noData++;
            }
        }

        textMonthSummary.setText(
                "📅 Monthly Summary\n\n" +
                        "🟢 Successful Days: " + success + "\n" +
                        "🔴 Failed Days: " + failed + "\n" +
                        "⚪ No Data / Future Days: " + noData
        );
    }

    private String format(int n) {
        return (n < 10 ? "0" : "") + n;
    }
}
