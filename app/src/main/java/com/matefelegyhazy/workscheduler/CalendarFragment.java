package com.matefelegyhazy.workscheduler;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TimePicker;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;
import com.prolificinteractive.materialcalendarview.format.DayFormatter;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CalendarFragment extends Fragment {
    private static final String DEFAULT_TIME_START_KEY = "default_time_start";
    private static final String DEFAULT_TIME_END_KEY = "default_time_end";
    private static final String ACTUAL_BACKGROUND_COLOR_KEY = "actualBackgroundColor";
    private static final String DEFAULT_BACKGROUND_COLOR_KEY = "defaultBackgroundColor";

    private MaterialCalendarView calendarView;
    private SelectedDatesAdapter selectedDatesAdapter;
    private ArrayList<CustomDayViewDecorator> decorators;
    private boolean isFirstStart;
    private String displayedYearAndMonth;

    public interface OnDisplayedMonthChanged {
        public void onDisplayedYearAndMonth(String displayedYearAndMonth);
        public void onDisplayedYearAndMonth(int displayedYear, int displayedMonth);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (isFirstStart) {
            isFirstStart = false;
            loadItemsInBackground();
        }
        refreshCalendarView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        decorators = new ArrayList<>();
        selectedDatesAdapter = SelectedDatesAdapter.getInstance();

        isFirstStart = true;
    }

    private void loadItemsInBackground() {
        new AsyncTask<Void, Void, List<SelectedDate>>() {

            @Override
            protected List<SelectedDate> doInBackground(Void... voids) {
                return SelectedDate.listAll(SelectedDate.class);
            }

            @Override
            protected void onPostExecute(List<SelectedDate> selectedDates) {
                super.onPostExecute(selectedDates);
                selectedDatesAdapter.update(selectedDates);
                setDatesSelected(selectedDates);
                initializeDecorators(selectedDates);
                refreshCalendarView();
            }
        }.execute();
    }

    private void setDatesSelected(List<SelectedDate> selectedDates) {
        for (SelectedDate selectedDate: selectedDates) {
            calendarView.setDateSelected(selectedDate.getDate(),true);
        }
    }

    private void initializeDecorators(List<SelectedDate> selectedDates) {
        for (SelectedDate selectedDate : selectedDates) {
            CustomDayViewDecorator newDecorator = new CustomDayViewDecorator(selectedDate.getDate(),selectedDate.getColor());
            decorators.add(newDecorator);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = (MaterialCalendarView) view.findViewById(R.id.calendarView);
        initializeCalendarView();

        return view;
    }

    private void initializeCalendarView() {
        calendarView.setDayFormatter(new DayFormatter() {
            @NonNull
            @Override
            public String format(@NonNull CalendarDay calendarDay) {
                if (selectedDatesAdapter.contains(calendarDay.getDate())) {
                  return selectedDatesAdapter.getDayText(calendarDay);
                }
                else {
                    return Integer.toString(calendarDay.getDay());
                }
            }
        });

        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_MULTIPLE);

        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
                @Override
                public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                    if (selected) {
                        createSelectedDate(date);
                    }
                    else {
                        int removableIndex = selectedDatesAdapter.remove(date);
                        decorators.remove(removableIndex);
                        refreshCalendarView();
                    }
                }
        });

        calendarView.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay calendarDay) {
                int monthIndex = calendarDay.getMonth();
                String displayedMonthName = new DateFormatSymbols().getMonths()[monthIndex];
                displayedYearAndMonth = calendarDay.getYear() + " " + displayedMonthName;
                ((OnDisplayedMonthChanged)getActivity()).onDisplayedYearAndMonth(displayedYearAndMonth);
                ((OnDisplayedMonthChanged)SelectedDatesAdapter.getInstance()).onDisplayedYearAndMonth(calendarDay.getYear(),monthIndex);
            }
        });

        int monthIndex = calendarView.getCurrentDate().getMonth();
        String displayedMonthName = new DateFormatSymbols().getMonths()[monthIndex];
        displayedYearAndMonth = calendarView.getCurrentDate().getYear() + " " + displayedMonthName;
        ((OnDisplayedMonthChanged)getActivity()).onDisplayedYearAndMonth(displayedYearAndMonth);
        ((OnDisplayedMonthChanged)SelectedDatesAdapter.getInstance()).onDisplayedYearAndMonth(calendarView.getCurrentDate().getYear(),monthIndex);
    }

    private void createSelectedDate(final CalendarDay calendarDay) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        long startTime = sharedPreferences.getLong(DEFAULT_TIME_START_KEY,0);
        long startHour = TimeUnit.MILLISECONDS.toHours(startTime);
        startTime -= TimeUnit.HOURS.toMillis(startHour);
        long startMinute = TimeUnit.MILLISECONDS.toMinutes(startTime);

        long endTime = sharedPreferences.getLong(DEFAULT_TIME_END_KEY,0);
        final long endHour = TimeUnit.MILLISECONDS.toHours(endTime);
        endTime -= TimeUnit.HOURS.toMillis(endHour);
        final long endMinute = TimeUnit.MILLISECONDS.toMinutes(endTime);

        final int actualBackgroundColor = sharedPreferences.getInt(ACTUAL_BACKGROUND_COLOR_KEY,
                sharedPreferences.getInt(DEFAULT_BACKGROUND_COLOR_KEY,R.color.defaultBackgroundColor));

        final AtomicBoolean shouldShow = new AtomicBoolean(true);
        final AtomicInteger firstTime = new AtomicInteger(0);
        final AtomicInteger secondTime = new AtomicInteger(0);


        final TimePickerDialog firstTimePickerDialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(final TimePicker timePicker, int hours, int minutes) {
                final String start = String.format("%02d:%02d",hours,minutes);
                firstTime.set(hours*60+minutes);
                final TimePickerDialog secondTimePickerDialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hours, int minutes) {
                        secondTime.set(hours*60+minutes);

                        if (firstTime.get() < secondTime.get()) {
                            final String end = String.format("%02d:%02d", hours, minutes);
                            String text = start + "\n" + end;

                            SelectedDate newSelectedDate = new SelectedDate(calendarDay.getDate(), text, actualBackgroundColor);
                            CustomDayViewDecorator newDecorator = new CustomDayViewDecorator(calendarDay.getDate(), actualBackgroundColor);
                            decorators.add(newDecorator);
                            selectedDatesAdapter.add(newSelectedDate);

                            refreshCalendarView();
                        }
                        else {
                            calendarView.setDateSelected(calendarDay,false);
                            Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.unsuccessful_event_adding,
                                    Snackbar.LENGTH_LONG).show();
                        }
                    }
                },(int)endHour,(int)endMinute, DateFormat.is24HourFormat(getActivity()));

                secondTimePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        if (secondTime.get() == 0) {
                            String text = start;

                            SelectedDate newSelectedDate = new SelectedDate(calendarDay.getDate(),text,actualBackgroundColor);
                            CustomDayViewDecorator newDecorator = new CustomDayViewDecorator(calendarDay.getDate(),actualBackgroundColor);
                            decorators.add(newDecorator);
                            selectedDatesAdapter.add(newSelectedDate);

                            refreshCalendarView();
                        }
                    }
                });

                secondTimePickerDialog.setTitle(R.string.end_time);

                if (shouldShow.get()) {
                    shouldShow.set(false);
                    secondTimePickerDialog.show();
                }
            }
        },(int)startHour,(int)startMinute, DateFormat.is24HourFormat(getActivity()));

        firstTimePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (firstTime.get() == 0) {
                    String text = Integer.toString(calendarDay.getDay());

                    SelectedDate newSelectedDate = new SelectedDate(calendarDay.getDate(), text, actualBackgroundColor);
                    CustomDayViewDecorator newDecorator = new CustomDayViewDecorator(calendarDay.getDate(),actualBackgroundColor);

                    decorators.add(newDecorator);
                    selectedDatesAdapter.add(newSelectedDate);

                    refreshCalendarView();
                }
            }
        });

        firstTimePickerDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                firstTime.set(0);
                secondTime.set(0);
            }
        });

        firstTimePickerDialog.setTitle(R.string.start_time);
        firstTimePickerDialog.show();
    }

    private void refreshCalendarView() {
        calendarView.removeDecorators();
        calendarView.addDecorators(decorators);
    }
}
