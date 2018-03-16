package com.matefelegyhazy.workscheduler;

import com.orm.SugarRecord;

import java.util.Date;

public class SelectedDate extends SugarRecord{
    private Date date;
    private String text;
    private int color;

    public SelectedDate(){}

    public SelectedDate(Date date, String text, int color) {
        this.date = date;
        this.text = text;
        this.color = color;
    }

    public Date getDate() { return this.date; }

    public String getText() {
        return this.text;
    }

    public int getColor() {
        return this.color;
    }
}
