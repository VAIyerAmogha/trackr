package com.example.trackr;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    TextView textTotalHabits, textCurrentStreak, textLongestStreak, textBadge, textBadgeEmoji;
    DBHelper dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        textTotalHabits = view.findViewById(R.id.textTotalHabits);
        textCurrentStreak = view.findViewById(R.id.textCurrentStreak);
        textLongestStreak = view.findViewById(R.id.textLongestStreak);
        textBadge = view.findViewById(R.id.textBadge);
        textBadgeEmoji = view.findViewById(R.id.textBadgeEmoji);

        dbHelper = new DBHelper(getContext());
        loadProfileData();

        return view;
    }

    private void loadProfileData() {
        Cursor habits = dbHelper.getAllHabits();
        int total = habits.getCount();
        habits.close();

        Cursor streak = dbHelper.getStreakInfo();
        int current = 0;
        int longest = 0;
        if (streak.moveToFirst()) {
            current = streak.getInt(streak.getColumnIndexOrThrow("current_streak"));
            longest = streak.getInt(streak.getColumnIndexOrThrow("longest_streak"));
        }
        streak.close();

        textTotalHabits.setText("Total Habits: " + total);
        textCurrentStreak.setText("Current Streak: " + current);
        textLongestStreak.setText("Longest Streak: " + longest);
        textBadge.setText("Badge: " + getBadgeName(longest));
        textBadgeEmoji.setText(getBadgeEmoji(longest));
    }

    private String getBadgeName(int days) {
        if (days >= 100) return "Gold (100 Days)";
        if (days >= 30) return "Silver (30 Days)";
        if (days >= 7) return "Bronze (7 Days)";
        return "None";
    }

    private String getBadgeEmoji(int days) {
        if (days >= 100) return "🥇";
        if (days >= 30) return "🥈";
        if (days >= 7)  return "🥉";
        return "🏅";
    }
}
