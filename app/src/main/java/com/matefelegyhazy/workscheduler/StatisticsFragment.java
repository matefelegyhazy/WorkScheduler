package com.matefelegyhazy.workscheduler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class StatisticsFragment extends Fragment {

    private static final String HOURLY_PAYMENT_ON_WEEKDAYS_KEY = "hourly_payment_on_weekdays";
    private static final String HOURLY_PAYMENT_ON_WEEKENDS_KEY = "hourly_payment_on_weekends";
    private PieChart chartMoney;
    private SelectedDatesAdapter datesAdapter;

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser){
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser) {
            loadData();
            chartMoney.invalidate();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        datesAdapter = SelectedDatesAdapter.getInstance();

        chartMoney = (PieChart) view.findViewById(R.id.chartMoney);

        return view;
    }

    private void loadData() {
        datesAdapter = SelectedDatesAdapter.getInstance();
        List<PieEntry> entries = new ArrayList<>();
        PieDataSet dataSet;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String paymentOnWeekdays = sharedPreferences.getString(HOURLY_PAYMENT_ON_WEEKDAYS_KEY,"");
        String paymentOnWeekends = sharedPreferences.getString(HOURLY_PAYMENT_ON_WEEKENDS_KEY,"");

        if(!paymentOnWeekdays.equals("") && !paymentOnWeekends.equals("")) {
            entries.add(new PieEntry(datesAdapter.getSelectedWeekendDaysSumHours()*Integer.parseInt(paymentOnWeekends), getString(R.string.income_weekends)));
            entries.add(new PieEntry(datesAdapter.getSelectedWeekdaysSumHours()*Integer.parseInt(paymentOnWeekdays), getString(R.string.income_weekdays)));
            dataSet = new PieDataSet(entries, getString(R.string.income));
        }
        else {
            entries.add(new PieEntry(datesAdapter.getSelectedWeekendDaysSumHours(), getString(R.string.weekends)));
            entries.add(new PieEntry(datesAdapter.getSelectedWeekdaysSumHours(), getString(R.string.weekdays)));

            dataSet = new PieDataSet(entries, getString(R.string.worked_days));
        }

        dataSet.setValueTextSize(20);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        PieData data = new PieData(dataSet);
        chartMoney.setData(data);
        chartMoney.invalidate();
    }
}
