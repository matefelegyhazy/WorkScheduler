package com.matefelegyhazy.workscheduler;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity
        implements ColorPickerDialogListener, CalendarFragment.OnDisplayedMonthChanged {

    private static final int PERMISSION_REQUEST_WRITE_CALENDAR = 0;
    private static final int SELECTION_COLOR_DIALOG_ID = 0;

    private static final String ACTUAL_BACKGROUND_COLOR_KEY = "actualBackgroundColor";
    private static final String DEFAULT_BACKGROUND_COLOR_KEY = "defaultBackgroundColor";
    private static final String EMAIL_RECIPIENT_KEY = "email_recipient";
    private static final String EMAIL_SUBJECT_KEY = "email_subject";
    private static final String EVENT_NAME_KEY = "eventName";
    private static final String LOCAL_CALENDARS_KEY = "localCalendars";
    private static final String DEFAULT_TIME_START_KEY = "default_time_start";
    private static final String DEFAULT_TIME_END_KEY = "default_time_end";
    private static final long DEFAULT_TIME_START = 28800000;
    private static final long DEFAULT_TIME_END = 57600000;
    private static final int DEFAULT_BACKGROUND_COLOR = -54125;
    private static final String FIRST_START_KEY = "firstStart";

    private Toolbar toolbar;
    private BottomNavigationView bottomNavigationView;
    private ViewPager viewPager;
    private PagerAdapter fragmentPagerAdapter;
    private MenuItem previousMenuItem;
    private String displayedYearAndMonth;
    private String calendarName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        setupBottomNavigationView(bottomNavigationView);

        viewPager = (ViewPager) findViewById(R.id.viewPager);
        setupViewPager(viewPager);

        initializeSharedPreferences();
    }

    private void initializeSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        int firstStart = sharedPreferences.getInt(FIRST_START_KEY,1);
        if (firstStart == 1) {
            sharedPreferences.edit().putInt(FIRST_START_KEY,0).apply();
            sharedPreferences.edit().putLong(DEFAULT_TIME_START_KEY, DEFAULT_TIME_START).apply();
            sharedPreferences.edit().putLong(DEFAULT_TIME_END_KEY, DEFAULT_TIME_END).apply();
            sharedPreferences.edit().putInt(DEFAULT_BACKGROUND_COLOR_KEY, DEFAULT_BACKGROUND_COLOR).apply();
        }
    }

    private void setupBottomNavigationView(final BottomNavigationView bottomNavigationView) {
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_calendar:
                        viewPager.setCurrentItem(PagerAdapter.NUM_CALENDAR);
                        return true;
                    case R.id.navigation_statistics:
                        viewPager.setCurrentItem(PagerAdapter.NUM_STATISTICS);
                        return true;
                    case R.id.navigation_settings:
                        viewPager.setCurrentItem(PagerAdapter.NUM_SETTINGS);
                        return true;
                }
                return false;
            }
        });
    }

    private void setupViewPager(ViewPager viewPager) {
        fragmentPagerAdapter = new PagerAdapter(getFragmentManager());
        viewPager.setAdapter(fragmentPagerAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                supportInvalidateOptionsMenu();
                if (previousMenuItem != null) {
                    previousMenuItem.setChecked(false);
                }
                else
                {
                    bottomNavigationView.getMenu().getItem(0).setChecked(false);
                }

                bottomNavigationView.getMenu().getItem(position).setChecked(true);
                previousMenuItem = bottomNavigationView.getMenu().getItem(position);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final Menu toolbarMenu = toolbar.getMenu();
        getMenuInflater().inflate(R.menu.menu_toolbar, toolbarMenu);
        if(viewPager.getCurrentItem() == PagerAdapter.NUM_CALENDAR) {
            menu.findItem(R.id.action_change_color).setVisible(true);
            menu.findItem(R.id.action_export_to_calendar).setVisible(true);
            menu.findItem(R.id.action_send_email).setVisible(true);
        }
        else {
            menu.findItem(R.id.action_change_color).setVisible(false);
            menu.findItem(R.id.action_export_to_calendar).setVisible(false);
            menu.findItem(R.id.action_send_email).setVisible(false);
        }
        for (int i = 0; i < toolbarMenu.size(); i++) {
            final MenuItem menuItem = toolbarMenu.getItem(i);
            menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(final MenuItem item) {
                    return onOptionsItemSelected(item);
                }
            });
            if (menuItem.hasSubMenu()) {
                final SubMenu subMenu = menuItem.getSubMenu();
                for (int j = 0; j < subMenu.size(); j++) {
                    subMenu.getItem(j).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(final MenuItem item) {
                            return onOptionsItemSelected(item);
                        }
                    });
                }
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_change_color:
                item.setChecked(true);
                createActualBackgroundColorPickerDialog();
                return true;
            case R.id.action_export_to_calendar:
                item.setChecked(true);
                ActionExportToCalendar();
                return true;
            case R.id.action_send_email:
                item.setChecked(true);
                ActionSendEmail();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void ActionExportToCalendar() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);

        calendarName = PreferenceManager.getDefaultSharedPreferences(this).getString(LOCAL_CALENDARS_KEY,"");
        if (calendarName.equals("")) {
            alertDialogBuilder.setMessage(R.string.no_chosen_calendar)
                    .setCancelable(false)
                    .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        else {
            alertDialogBuilder
                    .setMessage(getResources().getString(R.string.export_are_you_sure, displayedYearAndMonth, calendarName))
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_CALENDAR)
                                    != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.WRITE_CALENDAR},
                                        PERMISSION_REQUEST_WRITE_CALENDAR);
                            } else {
                                exportToCalendar();
                            }
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();

            alertDialog.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_WRITE_CALENDAR) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(android.R.id.content), R.string.permission_granted_calendar,
                        Snackbar.LENGTH_SHORT)
                        .show();

                exportToCalendar();
            } else {
                Snackbar.make(findViewById(android.R.id.content), R.string.permission_denied_calendar,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void exportToCalendar() {
           new AsyncTask<Void, Void, List<Long>>() {
                ContentResolver contentResolver;
                int calendarID;
                String title;
                String timeZoneID;

                @Override
                protected void onPreExecute() {
                    contentResolver = getContentResolver();

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    calendarID = sharedPreferences.getInt(calendarName,-1);
                    title = sharedPreferences.getString(EVENT_NAME_KEY,getString(R.string.event));

                    timeZoneID = TimeZone.getDefault().getID();
                }
                @Override
                protected List<Long> doInBackground(Void... voids) {
                    List<Long> eventIDs = new ArrayList<Long>();

                    ContentValues contentValues = new ContentValues();
                    List<SelectedDate> events = SelectedDatesAdapter.getInstance().getDisplayedMonthEvents();

                    eventIDs.add((long)events.size());

                    for (SelectedDate event: events) {
                        String[] splittedTime = event.getText().split("\n");
                        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("HH:mm");
                        dateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

                        Date startTime = new Date(0);
                        Date endTime = new Date(0);
                        try {
                            startTime = dateTimeFormat.parse(splittedTime[0]);
                            endTime = dateTimeFormat.parse(splittedTime[1]);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        long startDate = event.getDate().getTime() + startTime.getTime();
                        long endDate = event.getDate().getTime() + endTime.getTime();

                        contentValues.put(CalendarContract.Events.DTSTART, startDate);
                        contentValues.put(CalendarContract.Events.DTEND, endDate);
                        contentValues.put(CalendarContract.Events.TITLE, title);
                        contentValues.put(CalendarContract.Events.CALENDAR_ID, calendarID);
                        contentValues.put(CalendarContract.Events.EVENT_TIMEZONE, timeZoneID);

                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CALENDAR)
                                == PackageManager.PERMISSION_GRANTED) {
                            Uri uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, contentValues);
                            long eventID = Long.parseLong(uri.getLastPathSegment());
                            eventIDs.add(eventID);
                        }
                    }

                    return eventIDs;
                }

                @Override
                protected void onPostExecute(final List<Long> eventIDs) {
                    super.onPostExecute(eventIDs);
                     {
                         if (eventIDs.get(0).equals(new Long(eventIDs.size()-1))) {
                             eventIDs.remove(0);
                             Snackbar.make(findViewById(android.R.id.content), R.string.export_successful,
                                     Snackbar.LENGTH_LONG).setAction(getString(R.string.undo), new View.OnClickListener() {
                                 @Override
                                 public void onClick(View view) {
                                     for (Long eventID: eventIDs) {
                                        Uri deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID);
                                        int rows = contentResolver.delete(deleteUri, null, null);
                                    }
                                 }
                             }).show();
                         }
                         else {
                             eventIDs.remove(0);
                             Snackbar.make(findViewById(android.R.id.content), R.string.export_unsuccessful,
                                     Snackbar.LENGTH_LONG).show();
                             for (Long eventID: eventIDs) {
                                 Uri deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID);
                                 int rows = contentResolver.delete(deleteUri, null, null);
                             }
                         }
                    }
                }
            }.execute();

    }

    private void createActualBackgroundColorPickerDialog() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        final int actualBackgroundColor = sharedPreferences.getInt(ACTUAL_BACKGROUND_COLOR_KEY,
                sharedPreferences.getInt(DEFAULT_BACKGROUND_COLOR_KEY,R.color.defaultBackgroundColor));
        ColorPickerDialog.newBuilder()
                .setDialogType(ColorPickerDialog.TYPE_PRESETS)
                .setAllowPresets(true)
                .setDialogId(SELECTION_COLOR_DIALOG_ID)
                .setColor(actualBackgroundColor)
                .setDialogTitle(R.string.select_default_background_color)
                .setCustomButtonText(R.string.colorpicker_custom_buttom)
                .setSelectedButtonText(R.string.colorpicker_selected_button)
                .setPresetsButtonText(R.string.colorpicker_presets_button)
                .setShowAlphaSlider(false)
                .show(this);
    }

    private void ActionSendEmail() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        String recipient = sharedPreferences.getString(EMAIL_RECIPIENT_KEY,"");
        String subject = sharedPreferences.getString(EMAIL_SUBJECT_KEY,"");

        List<SelectedDate> events = SelectedDatesAdapter.getInstance().getDisplayedMonthAllEvents();
        String text = getTextForEmail(events);

        Intent sendEmailIntent = new Intent(Intent.ACTION_SEND);
        sendEmailIntent.setType("*/*");
        sendEmailIntent.putExtra(Intent.EXTRA_EMAIL,new String[] {recipient});
        sendEmailIntent.putExtra(Intent.EXTRA_SUBJECT,subject);
        sendEmailIntent.putExtra(Intent.EXTRA_TEXT,text);
        startActivity(sendEmailIntent);

    }

    private String getTextForEmail(List<SelectedDate> events) {
        String emailText = "";

        for (SelectedDate event: events) {
            String time = event.getText().length() > 2 ? " " + event.getText() : "";
            time = time.replace("\r\n"," ").replace("\n","-");
            String monthAndDay = String.format("%02d.%02d",event.getDate().getMonth()+1,event.getDate().getDate());
            emailText += monthAndDay + time + "\n";
        }

        return emailText;
    }

    @Override
    public void onColorSelected(int dialogId, @ColorInt int color) {
        switch (dialogId) {
            case SELECTION_COLOR_DIALOG_ID:
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                sharedPreferences.edit().putInt(ACTUAL_BACKGROUND_COLOR_KEY,color).commit();
                break;
        }
    }

    @Override
    public void onDialogDismissed(int dialogId) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        int defaultBackgroundColor = sharedPreferences.getInt(DEFAULT_BACKGROUND_COLOR_KEY,R.color.defaultBackgroundColor);
        sharedPreferences.edit().putInt(ACTUAL_BACKGROUND_COLOR_KEY,defaultBackgroundColor).apply();
    }

    @Override
    public void onDisplayedYearAndMonth(String displayedYearAndMonth) {
        this.displayedYearAndMonth = displayedYearAndMonth;
    }

    @Override
    public void onDisplayedYearAndMonth(int displayedYear, int displayedMonth) {}
}