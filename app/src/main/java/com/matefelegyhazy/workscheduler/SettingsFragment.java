package com.matefelegyhazy.workscheduler;

import android.Manifest;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.CalendarContract;
import android.support.design.widget.Snackbar;
import android.support.v13.app.ActivityCompat;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Map;

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener,  ActivityCompat.OnRequestPermissionsResultCallback{


    private static final String ACTUAL_BACKGROUND_COLOR_KEY = "actualBackgroundColor";
    private static final String DEFAULT_BACKGROUND_COLOR_KEY = "defaultBackgroundColor";
    private static final String FIRST_START_KEY = "firstStart";

    private static final int PERMISSION_REQUEST_READ_CALENDAR = 0;
    private SharedPreferences sharedPreferences;
    private ArrayList<String> exceptions;

    static final int[] WRITE_ACCESS = new int[]{
            CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
            CalendarContract.Calendars.CAL_ACCESS_EDITOR,
            CalendarContract.Calendars.CAL_ACCESS_OWNER,
            CalendarContract.Calendars.CAL_ACCESS_ROOT};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        setExceptions();
        initializeListPreferences();
        initializePreferencesSummaries();

    }

    private void initializeListPreferences() {
        initializeLocalCalendarsListPreference();
    }

    private void initializeLocalCalendarsListPreference() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            FragmentCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALENDAR},
                    PERMISSION_REQUEST_READ_CALENDAR);
        }
        else {
            uploadLocalCalendarsListPreference();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_READ_CALENDAR) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.permission_granted_calendar,
                        Snackbar.LENGTH_SHORT)
                        .show();
                uploadLocalCalendarsListPreference();
            } else {
                Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.permission_denied_calendar,
                        Snackbar.LENGTH_SHORT)
                        .show();
                PreferenceScreen preferenceScreen = getPreferenceScreen();
                ListPreference localCalendarsListPreference = (ListPreference) preferenceScreen.findPreference("localCalendars");
                localCalendarsListPreference.setEnabled(false);
            }
        }
    }

    private void uploadLocalCalendarsListPreference() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            ListPreference localCalendarsListPreference = (ListPreference) preferenceScreen.findPreference("localCalendars");

            final String[] projection = new String[]{
                    CalendarContract.Calendars._ID,                     // 0
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,   // 1
                    CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL    // 2
            };

            final int PROJECTION_ID_INDEX = 0;
            final int PROJECTION_DISPLAY_NAME_INDEX = 1;
            final int PROJECTION_ACCESS_LEVEL_INDEX = 2;

            final ContentResolver contentResolver = getActivity().getContentResolver();
            final Uri uri = CalendarContract.Calendars.CONTENT_URI;
            Cursor cursor = contentResolver.query(uri, projection, null, null, null);

            int maxIndex = 0;
            while (cursor.moveToNext()) {
                if (hasWriteAccess(cursor.getInt(PROJECTION_ACCESS_LEVEL_INDEX))) {
                    ++maxIndex;
                }
            }

            CharSequence[] entryValues = new CharSequence[maxIndex];

            int index = 0;
            while(cursor.moveToPrevious() && index < maxIndex)  {
                    if(hasWriteAccess(cursor.getInt(PROJECTION_ACCESS_LEVEL_INDEX))) {
                        long id = cursor.getLong(PROJECTION_ID_INDEX);
                        entryValues[index] = cursor.getString(PROJECTION_DISPLAY_NAME_INDEX);
                        sharedPreferences.edit().putInt(cursor.getString(PROJECTION_DISPLAY_NAME_INDEX),(int)id).commit();
                        ++index;
                    }
            }
            localCalendarsListPreference.setEntries(entryValues);
            localCalendarsListPreference.setEntryValues(entryValues);

            localCalendarsListPreference.setEnabled(true);
        }
    }

    private boolean hasWriteAccess(int accessLevel){
        for (int i = 0; i < WRITE_ACCESS.length; ++i) if(WRITE_ACCESS[i] == accessLevel) return true;
        return false;
    }

    private void setExceptions() {
        exceptions = new ArrayList<>();
        exceptions.add(DEFAULT_BACKGROUND_COLOR_KEY);
        exceptions.add(ACTUAL_BACKGROUND_COLOR_KEY);
        exceptions.add(FIRST_START_KEY);
    }

    private void initializePreferencesSummaries() {
        Map<String, ?> preferences = sharedPreferences.getAll();

        for (Map.Entry<String, ?> entry : preferences.entrySet()) {
            if (!exceptions.contains(entry.getKey())) {
                Preference preference = findPreference(entry.getKey());
                if (preference != null) {
                    preference.setSummary(entry.getValue().toString());
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        initializePreferencesSummaries();
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference changedPreference = findPreference(key);

        Map<String, ?> preferences = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : preferences.entrySet()) {
            if (!exceptions.contains(key) && entry.getKey().equals(key) && changedPreference != null)
                changedPreference.setSummary(entry.getValue().toString());
        }
    }
}