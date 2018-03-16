package com.matefelegyhazy.workscheduler;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.IntegerRes;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.TimeZone;

public class TimePickerFragment extends DialogFragment
                            implements TimePickerDialog.OnTimeSetListener {

    private String text;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance(TimeZone.getDefault());
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        this.text = hourOfDay + ":" + minute;
    }

    public String getText() {
        return this.text;
    }
}