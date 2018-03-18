package com.matefelegyhazy.workscheduler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

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
import com.thomashaertel.widget.MultiSpinner;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class StatisticsFragment extends Fragment {

    private static final String HOURLY_PAYMENT_ON_WEEKDAYS_KEY = "hourly_payment_on_weekdays";
    private static final String HOURLY_PAYMENT_ON_WEEKENDS_KEY = "hourly_payment_on_weekends";

    private static final String WEEKDAY_SUM_HOURS = "weekdaySumHours";
    private static final String WEEKEND_SUM_HOURS = "weekendSumHours";
    private static final String SUM_HOURS = "sumHours";
    private static final String WEEKDAY_INCOME = "weekDayIncome";
    private static final String WEEKEND_INCOME = "weekEndIncome";
    private static final String SUM_INCOME = "sumIncome";

    private MultiSpinner spinner;
    private ArrayAdapter<String> spinnerAdapter;
    private boolean[] selectedItems;
    private final List<String> selectedMonths = new ArrayList<>();

    private SelectedDatesAdapter datesAdapter;

    private TextView tvWeekdaySumHours;
    private TextView tvWeekendSumHours;
    private TextView tvSumHours;

    private TextView tvWeekdayIncome;
    private TextView tvWeekendIncome;
    private TextView tvSumIncome;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        tvWeekdaySumHours = view.findViewById(R.id.tvWeekdaySumHours);
        tvWeekendSumHours = view.findViewById(R.id.tvWeekendSumHours);
        tvSumHours = view.findViewById(R.id.tvSumHours);

        tvWeekdayIncome = view.findViewById(R.id.tvWeekdayIncome);
        tvWeekendIncome = view.findViewById(R.id.tvWeekendIncome);
        tvSumIncome = view.findViewById(R.id.tvSumIncome);

        datesAdapter = SelectedDatesAdapter.getInstance();
        spinnerAdapter = new ArrayAdapter<String>(getActivity(), R.layout.support_simple_spinner_dropdown_item);

        spinner = (MultiSpinner) view.findViewById(R.id.multiSpinner);
        spinner.setAdapter(spinnerAdapter, false, onSelectedListener);

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            new AsyncTask<Void, Void, List<String>>() {

                @Override
                protected List<String> doInBackground(Void... voids) {
                    return datesAdapter.getRelevantMonths();
                }

                @Override
                protected void onPostExecute(List<String> months) {
                    super.onPostExecute(months);

                    spinnerAdapter.clear();
                    spinnerAdapter.addAll(months);

                    selectedItems = new boolean[spinnerAdapter.getCount()];
                    if (spinnerAdapter.getCount() != 0) {
                        selectedItems[0] = true;
                        selectedMonths.add(spinnerAdapter.getItem(0));
                    }

                    spinner.setSelected(selectedItems);

                    updateData();
                }
            }.execute();


        }
    }

    private MultiSpinner.MultiSpinnerListener onSelectedListener = new MultiSpinner.MultiSpinnerListener() {
        @Override
        public void onItemsSelected(boolean[] selected) {
            for (int i = 0; i < selected.length; ++i) {
                if (selected[i] && !selectedMonths.contains(spinnerAdapter.getItem(i))) {
                    selectedMonths.add(spinnerAdapter.getItem(i));
                } else if (!selected[i] && selectedMonths.contains(spinnerAdapter.getItem(i))) {
                    selectedMonths.remove(spinnerAdapter.getItem(i));
                }
            }

            updateData();
        }
    };

    private void updateData() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String paymentOnWeekdaysPreference = sharedPreferences.getString(HOURLY_PAYMENT_ON_WEEKDAYS_KEY, null);
        String paymentOnWeekendsPreference = sharedPreferences.getString(HOURLY_PAYMENT_ON_WEEKENDS_KEY, null);

        final int paymentOnWeekdays = paymentOnWeekdaysPreference == null || paymentOnWeekdaysPreference.equals("") ? 0 : Integer.parseInt(paymentOnWeekdaysPreference);
        final int paymentOnWeekends = paymentOnWeekendsPreference == null || paymentOnWeekendsPreference.equals("") ? 0 : Integer.parseInt(paymentOnWeekendsPreference);

        new AsyncTask<Void, Void, HashMap<String, Float>>() {
            @Override
            protected HashMap<String, Float> doInBackground(Void... voids) {
                HashMap<String, Float> result = new HashMap<>();

                float weekdaySumHours = datesAdapter.getWeekdayEventsSumHours(selectedMonths);
                result.put(WEEKDAY_SUM_HOURS, weekdaySumHours);

                float weekendSumHours = datesAdapter.getWeekendEventsSumHours(selectedMonths);
                result.put(WEEKEND_SUM_HOURS, weekendSumHours);

                float weekdayIncome = weekdaySumHours * paymentOnWeekdays;
                result.put(WEEKDAY_INCOME, weekdayIncome);

                float weekendIncome = weekendSumHours * paymentOnWeekends;
                result.put(WEEKEND_INCOME, weekendIncome);

                float sumHours = weekdaySumHours + weekendSumHours;
                result.put(SUM_HOURS, sumHours);
                float sumIncome = weekdayIncome + weekendIncome;
                result.put(SUM_INCOME, sumIncome);

                return result;
            }

            @Override
            protected void onPostExecute(HashMap<String, Float> result) {
                super.onPostExecute(result);

                tvWeekdaySumHours.setText(Float.toString(result.get(WEEKDAY_SUM_HOURS)));
                tvWeekendSumHours.setText(Float.toString(result.get(WEEKEND_SUM_HOURS)));
                tvSumHours.setText(Float.toString(result.get(SUM_HOURS)));

                tvWeekdayIncome.setText(Float.toString(result.get(WEEKDAY_INCOME)));
                tvWeekendIncome.setText(Float.toString(result.get(WEEKEND_INCOME)));
                tvSumIncome.setText(Float.toString(result.get(SUM_INCOME)));

                spinner.setText("");
            }
        }.execute();
    }
}
