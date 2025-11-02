package com.example.trackr;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class ReportsFragment extends Fragment {

    DBHelper dbHelper;
    BarChart barChart;
    LineChart lineChart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        barChart = view.findViewById(R.id.barChart);
        lineChart = view.findViewById(R.id.lineChart);
        dbHelper = new DBHelper(getContext());

        setupWeeklyChart();
        setupMonthlyChart();

        return view;
    }

    private void setupWeeklyChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Last 7 days (including today)
        for (int i = 6; i >= 0; i--) {
            cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -i);
            String date = sdf.format(cal.getTime());
            float percentage = getCompletionPercentage(date);
            entries.add(new BarEntry(6 - i, percentage));
            labels.add(getDayLabel(i));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Completion %");
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setLabelCount(7);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void setupMonthlyChart() {
        ArrayList<Entry> entries = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Last 30 days
        for (int i = 29; i >= 0; i--) {
            cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -i);
            String date = sdf.format(cal.getTime());
            float percentage = getCompletionPercentage(date);
            entries.add(new Entry(29 - i, percentage));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily %");
        dataSet.setValueTextSize(8f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleRadius(3f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(dataSet);
        lineChart.setData(data);
        lineChart.getDescription().setEnabled(false);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    // Calculate how many habits were done out of total
    private float getCompletionPercentage(String date) {
        Cursor all = dbHelper.getAllHabits();
        int total = all.getCount();
        all.close();

        Cursor done = dbHelper.getHabitsWithDateStatus(date);
        int completed = 0;
        if (done.moveToFirst()) {
            do {
                int doneToday = done.getInt(done.getColumnIndexOrThrow("done_today"));
                if (doneToday == 1) completed++;
            } while (done.moveToNext());
        }
        done.close();

        if (total == 0) return 0;
        return (completed * 100f / total);
    }

    private String getDayLabel(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return new SimpleDateFormat("EEE", Locale.getDefault()).format(cal.getTime());
    }
}
