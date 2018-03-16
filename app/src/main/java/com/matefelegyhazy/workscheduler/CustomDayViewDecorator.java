package com.matefelegyhazy.workscheduler;

import android.annotation.TargetApi;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.util.Log;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.ArrayList;
import java.util.Date;

public class CustomDayViewDecorator implements DayViewDecorator {
    public final static int FADE_TIME = 200;
    private Date date;
    private int color;

    public CustomDayViewDecorator(Date date, int color) {
        this.date = date;
        this.color = color;
    }

    @Override
    public boolean shouldDecorate(CalendarDay calendarDay) {
        return date.equals(calendarDay.getDate());
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.setSelectionDrawable(generateSelector());
    }

    private Drawable generateSelector() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.setExitFadeDuration(FADE_TIME);
        drawable.addState(new int[]{android.R.attr.state_checked}, generateCircleDrawable(color));
        drawable.addState(new int[]{android.R.attr.state_pressed}, generateCircleDrawable(color));
        drawable.addState(new int[]{}, generateCircleDrawable(Color.TRANSPARENT));
        return drawable;
    }

    private static Drawable generateCircleDrawable(final int color) {
        ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
        drawable.getPaint().setColor(color);
        return drawable;
    }
}
