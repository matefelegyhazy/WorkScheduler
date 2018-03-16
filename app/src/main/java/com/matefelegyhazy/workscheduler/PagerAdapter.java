package com.matefelegyhazy.workscheduler;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.util.Log;

public class PagerAdapter extends FragmentPagerAdapter {
    private static final int NUM_PAGES = 3;
    public static final int NUM_CALENDAR = 0;
    public static final int NUM_STATISTICS = 1;
    public static final int NUM_SETTINGS = 2;

    private CalendarFragment calendarFragment;
    private StatisticsFragment statisticsFragment;
    private SettingsFragment settingsFragment;

    public PagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                if (calendarFragment == null) {
                    return new CalendarFragment();
                }
                else {
                    return calendarFragment;
                }
            case 1:
                if (statisticsFragment == null) {
                    return new StatisticsFragment();
                }
                else {
                    return statisticsFragment;
                }
            case 2:
                if (settingsFragment == null) {
                    return new SettingsFragment();
                }
                else {
                    return settingsFragment;
                }
            default:
                if (calendarFragment == null) {
                    return new CalendarFragment();
                }
                else {
                    return calendarFragment;
                }
        }
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }
}
