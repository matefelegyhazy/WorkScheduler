package com.matefelegyhazy.workscheduler;

import java.util.Comparator;

public class SelectedDateComparator implements Comparator<SelectedDate> {

    @Override
        public int compare(SelectedDate selectedDate1, SelectedDate selectedDate2) {
            return (int)(selectedDate1.getDate().getTime() - selectedDate2.getDate().getTime());
        }
}
