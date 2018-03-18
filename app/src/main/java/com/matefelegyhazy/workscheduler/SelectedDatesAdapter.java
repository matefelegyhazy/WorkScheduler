package com.matefelegyhazy.workscheduler;

import android.app.Activity;
import android.util.Log;

import com.prolificinteractive.materialcalendarview.CalendarDay;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CancellationException;

public class SelectedDatesAdapter
        implements CalendarFragment.OnDisplayedMonthChanged {
    private ArrayList<SelectedDate> selectedDates;
    private static SelectedDatesAdapter instance = null;
    private int displayedMonth;
    private int displayedYear;
    private static long deltaTime = 60000;
    SelectedDateComparator comparator;

    private SelectedDatesAdapter() {
        selectedDates = new ArrayList<SelectedDate>();
        comparator = new SelectedDateComparator();
    }

    public static SelectedDatesAdapter getInstance() {
        if (instance == null) instance = new SelectedDatesAdapter();
        return instance;
    }

    public void add(SelectedDate selectedDate) {
        if (!contains(selectedDate.getDate()))
            selectedDates.add(selectedDate);
        selectedDate.save();
    }

    public int remove(CalendarDay calendarDay) {
        int removableIndex = -1;

        for (int i = 0; i < selectedDates.size(); ++i) {
            if (selectedDates.get(i).getDate().equals(calendarDay.getDate())) removableIndex = i;
        }

        selectedDates.get(removableIndex).delete();
        selectedDates.remove(removableIndex);

        return removableIndex;
    }

    public boolean contains(Date date) {
        for (SelectedDate selectedDate : selectedDates) {
            if (date.equals(selectedDate.getDate())) return true;
        }
        return false;
    }

    public String getDayText(CalendarDay calendarDay) {
        for (SelectedDate selectedDate : selectedDates) {
            if (calendarDay.getDate().equals(selectedDate.getDate())) return selectedDate.getText();
        }
        return "";
    }

    public void update(List<SelectedDate> persistedSelectedDates) {
        selectedDates.clear();
        selectedDates.addAll(persistedSelectedDates);
    }

    public List<String> getRelevantMonths() {
        List<String> relevantMonths = new ArrayList<>();

        for (SelectedDate selectedDate : selectedDates) {
            if (selectedDate.getText().contains("\n")) {
                Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                calendar.setTime(selectedDate.getDate());

                String result = new SimpleDateFormat("yyyy MMMM").format(calendar.getTime());
                if (!relevantMonths.contains(result))
                    relevantMonths.add(result);
            }
        }

        return relevantMonths;
    }

    public float getWeekdayEventsSumHours(List<String> filterDates) {
        float selectedWeekdaysSumHours = 0;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.setFirstDayOfWeek(Calendar.MONDAY);

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("HH:mm");

        for (SelectedDate selectedDate : selectedDates) {
            if (selectedDate.getText().contains("\n")) {
                calendar.setTime(selectedDate.getDate());
                int day = calendar.get(Calendar.DAY_OF_WEEK);
                String date = new SimpleDateFormat("yyyy MMMM").format(calendar.getTime());

                if (filterDates.contains(date) && day != Calendar.SATURDAY && day != Calendar.SUNDAY) {
                    String[] splittedTime = selectedDate.getText().split("\n");

                    Date startTime = new Date(0);
                    Date endTime = new Date(0);

                    try {
                        startTime = dateTimeFormat.parse(splittedTime[0]);
                        endTime = dateTimeFormat.parse(splittedTime[1]);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    selectedWeekdaysSumHours += (float) (endTime.getTime() - startTime.getTime()) / 1000 / 60 / 60;
                }
            }
        }

        return selectedWeekdaysSumHours;
    }

    public float getWeekendEventsSumHours(List<String> filterDates) {
        float selectedWeekendsSumHours = 0;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.setFirstDayOfWeek(Calendar.MONDAY);

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("HH:mm");

        for (SelectedDate selectedDate : selectedDates) {
            if (selectedDate.getText().contains("\n")) {
                calendar.setTime(selectedDate.getDate());
                int day = calendar.get(Calendar.DAY_OF_WEEK);
                String date = new SimpleDateFormat("yyyy MMMM").format(calendar.getTime());

                if (filterDates.contains(date) && (day == Calendar.SATURDAY || day == Calendar.SUNDAY)) {
                    String[] splittedTime = selectedDate.getText().split("\n");

                    Date startTime = new Date(0);
                    Date endTime = new Date(0);

                    try {
                        startTime = dateTimeFormat.parse(splittedTime[0]);
                        endTime = dateTimeFormat.parse(splittedTime[1]);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    selectedWeekendsSumHours += (float) (endTime.getTime() - startTime.getTime()) / 1000 / 60 / 60;
                }
            }
        }

        return selectedWeekendsSumHours;
    }

    public List<SelectedDate> getDisplayedMonthEvents() {
        List<SelectedDate> events = new ArrayList<SelectedDate>();

        for (SelectedDate selectedDate : selectedDates) {
            long eventTime = selectedDate.getDate().getTime();
            if (isInDisplayedMonth(eventTime) && selectedDate.getText().contains("\n"))
                events.add(selectedDate);
        }

        return events;
    }

    public List<SelectedDate> getDisplayedMonthAllEvents() {
        List<SelectedDate> events = new ArrayList<SelectedDate>();

        for (SelectedDate selectedDate : selectedDates) {
            long eventTime = selectedDate.getDate().getTime();
            if (isInDisplayedMonth(eventTime))
                events.add(selectedDate);
        }

        Collections.sort(events, comparator);
        return events;
    }

    private boolean isInDisplayedMonth(long eventTime) {
        long minTime = getMinTime();
        long maxTime = getMaxTime();

        return eventTime + deltaTime >= minTime && eventTime - deltaTime <= maxTime;
    }

    private long getMaxTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.set(Calendar.YEAR, displayedYear);
        calendar.set(Calendar.MONTH, displayedMonth);
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR, -12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        int lastDayOfMonth = calendar.getActualMaximum(Calendar.DATE);
        calendar.set(Calendar.DATE, lastDayOfMonth);
        long maxTime = calendar.getTime().getTime();

        return maxTime;
    }

    private long getMinTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.set(Calendar.YEAR, displayedYear);
        calendar.set(Calendar.MONTH, displayedMonth);
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR, -12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long minTime = calendar.getTime().getTime();

        return minTime;
    }

    @Override
    public void onDisplayedYearAndMonth(int displayedYear, int displayedMonth) {
        this.displayedMonth = displayedMonth;
        this.displayedYear = displayedYear;
    }

    @Override
    public void onDisplayedYearAndMonth(String displayedYearAndMonth) {
    }
}
