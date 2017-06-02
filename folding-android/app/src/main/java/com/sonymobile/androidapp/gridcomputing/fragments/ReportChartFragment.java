/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.fragments;


import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Pair;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.database.JobCheckpointsContract;
import com.sonymobile.androidapp.gridcomputing.log.Log;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ReportChartFragment extends Fragment {

    private static final int[] CHART_COLORS = new int[]{0xFFFFBF00, 0xFFA4C639,
                                                        0xFFFBCEB1, 0xFFA1CAF1,
                                                        0xFFFF9966, 0xFFDE5D83,
                                                        0xFFCD7F32};

    private DataType mDataType = DataType.WEEK;
    private TextView mContributedTimeTv;
    private LineChart mChart;
    private final CompoundButton.OnCheckedChangeListener mLegendCheckedChangeListener =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton buttonView,
                                             final boolean isChecked) {
                    final Object tag = buttonView.getTag();
                    if (tag != null && tag instanceof ILineDataSet) {
                        ((ILineDataSet) tag).setVisible(isChecked);
                        mChart.invalidate();
                    }
                }
            };
    private ProgressBar mProgressBar;
    private View mChartsLayout;
    private GridLayout mLegendLayout;

    public static ReportChartFragment newInstance(final int position) {
        if (position == 0) {
            return newInstance(DataType.WEEK);
        } else if (position == 1) {
            return newInstance(DataType.MONTH);
        } else {
            return newInstance(DataType.ALL_TIME);
        }
    }

    public static ReportChartFragment newInstance(final DataType dataType) {
        final ReportChartFragment fragment = new ReportChartFragment();
        fragment.mDataType = dataType;
        return fragment;
    }

    @Override
    public final View onCreateView(final LayoutInflater inflater,
                                   final ViewGroup container,
                                   final Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_report_chart, container, false);
        mChartsLayout = rootView.findViewById(R.id.fragment_report_charts_layout);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.fragment_report_progress);
        mContributedTimeTv = (TextView) rootView.findViewById(R.id.fragment_report_contributed_time);
        mChart = (LineChart) rootView.findViewById(R.id.fragment_report_chart);
        mLegendLayout = (GridLayout) rootView.findViewById(R.id.fragment_report_legend_layout);

        mChartsLayout.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        return rootView;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupChart();
        setChartValues();
    }

    private void setupChart() {
        final Resources resources = ApplicationData.getAppContext().getResources();
        final float density = getResources().getDisplayMetrics().density;
        final float smallTextSize = resources.getDimension(R.dimen.text_smallest) / density;

        final int yAxisTextColor = ContextCompat.getColor(ApplicationData.getAppContext(),
                                                          R.color.colorPrimary);

        final int xAxisTextColor = Color.BLACK;

        mChart.setDrawGridBackground(true);
        mChart.getDescription().setEnabled(false);
        mChart.setDrawBorders(false);
        mChart.setExtraOffsets(0, smallTextSize, 0, 0);


        mChart.getAxisRight().setEnabled(false);
        mChart.getAxisLeft().setDrawAxisLine(false);
        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getAxisLeft().setGranularity(1f);
        mChart.getAxisLeft().setAxisMinimum(0f);
        mChart.getAxisLeft().setTextColor(yAxisTextColor);
        mChart.getAxisLeft().setTextSize(smallTextSize);

        mChart.getXAxis().setDrawAxisLine(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.getXAxis().setGranularity(1f);
        mChart.getXAxis().setTextColor(xAxisTextColor);
        mChart.getXAxis().setTextSize(smallTextSize);

        mChart.getAxisLeft().setValueFormatter(new AxisHoursValueFormatter());

        if (mDataType == DataType.WEEK) {
            mChart.getXAxis().setValueFormatter(new AxisWeekValueFormatter());
        } else if (mDataType == DataType.MONTH) {
            mChart.getXAxis().setValueFormatter(new AxisWeekValueFormatter());
        } else if (mDataType == DataType.ALL_TIME) {
            mChart.getXAxis().setValueFormatter(new AxisYearValueFormatter());
        }


        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);

        mChart.getLegend().setEnabled(false);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);
    }

    private void setChartValues() {
        mChart.resetTracking();
        new AsyncTask<Void, Void, LineData>() {

            @Override
            protected LineData doInBackground(Void... params) {
                return getData();
            }

            @Override
            protected void onPostExecute(final LineData data) {
                try {
                    mChart.setData(data);
                    mChart.invalidate();
                    setLegends();
                    updateContributedTimeTitle();

                    mChartsLayout.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("Could not update the chart: " + e.getMessage());
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    /**
     * Creates a line data set to plot the chart.
     * @param setPosition this dataset position.
     * @param label The label of the data set.
     * @param array The sparse array containing the data to plot.
     * @param startValue the value at which the x axis starts.
     * @return a dataset ready to plot.
     */
    private LineDataSet getData(final int setPosition, final String label,
                                final SparseArray<Double> array,
                                final int startValue) {
        final List<Entry> values = new ArrayList<>();

        final int startIndex = startValue == 0 ? 0 : array.indexOfKey(startValue);


        for (int i = startIndex; i < array.size(); i++) {
            int key = array.keyAt(i);
            int xValue = startValue == 0 ? key : values.size() + startValue;
            values.add(new Entry(xValue, array.get(key).floatValue()));
        }
        for (int i = 0; i < startIndex; i++) {
            try {
                int key = array.keyAt(i);
                int xValue = startValue == 0 ? key : values.size() + startValue;
                values.add(new Entry(xValue, array.get(key).floatValue()));
            } catch (Exception e) {
                Log.e("Entry at index " + i + " does not exist");
            }
        }

        LineDataSet d = new LineDataSet(values, label);
        d.setLineWidth(2.5f);
        d.setCircleRadius(4f);
        d.setDrawValues(false);

        int color = CHART_COLORS[setPosition];
        d.setColor(color);
        d.setCircleColor(color);
        d.setDrawFilled(true);
        return d;
    }

    /**
     * Groups the original values into a sparse array.
     * The outer sparse array is indexed by the group field.
     * The inner sparse array is indexed by the index field.
     * If groupField and indexField are the same, then the outer sparse array contains only 1 entry
     * index by 0 and the inner sparse array is indexed by indexField.
     * @param original the original value returned from the SQL query.
     * @param groupField the field used to group the outer sparse array.
     * @param indexField the field used to group the inner sparse array.
     * @return a bidimensional sparse array indexed by  groupField and indexField.
     */
    private SparseArray<SparseArray<Double>> groupValues(
            final SparseArray<Pair<Date, Double>> original,
            final int groupField,
            final int indexField) {
        final Calendar calendar = Calendar.getInstance();
        final SparseArray<SparseArray<Double>> weeks = new SparseArray<>();

        for (int i = 0; i < original.size(); i++) {
            try {
                final int key = original.keyAt(i);

                calendar.setTime(original.get(key).first);
                final double value = original.get(key).second;


                final int indexValue = calendar.get(indexField);
                final int groupValue = groupField == indexField ? 0 : calendar.get(groupField);

                SparseArray<Double> currentWeek = weeks.get(groupValue);
                if (currentWeek == null) {
                    currentWeek = new SparseArray<>();
                    weeks.put(groupValue, currentWeek);
                }
                currentWeek.put(indexValue, value);
            } catch (Exception e) {
                Log.e(e.getMessage());
            }
        }

        // normalize the data
        // if the index field is DAY_OF_WEEK, then we'll add the remaining days to fill
        // the week from sunday until saturday
        if (indexField == Calendar.DAY_OF_WEEK) {
            for (int i = 0; i < weeks.size(); i++) {
                final int key = weeks.keyAt(i);
                SparseArray<Double> currentWeek = weeks.get(key);
                for (int j = Calendar.SUNDAY; j <= Calendar.SATURDAY; j++) {
                    if (currentWeek.get(j) == null) {
                        currentWeek.put(j, 0.0);
                    }
                }
            }
        }

        return weeks;
    }

    private LineData getData() {
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        final SparseArray<Pair<Date, Double>> sparseArray = JobCheckpointsContract
                .getHourlyReport(mDataType);

        if (mDataType == DataType.WEEK) {
            final SparseArray<SparseArray<Double>> weeksDays = groupValues(sparseArray,
                                                                           0,
                                                                           Calendar.DAY_OF_WEEK);
            if (weeksDays.size() > 0) {
                final Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, 1);
                final int startValue = calendar.get(Calendar.DAY_OF_WEEK);
                dataSets.add(getData(0, "", weeksDays.get(weeksDays.keyAt(0)), startValue));
            }
        } else if (mDataType == DataType.MONTH) {
            final SparseArray<SparseArray<Double>> weeksInMonth =
                    groupValues(sparseArray, Calendar.WEEK_OF_YEAR, Calendar.DAY_OF_WEEK);
            for (int i = 0; i < weeksInMonth.size(); i++) {
                final String label = getString(R.string.chart_week_label, i + 1);
                dataSets.add(getData(i, label, weeksInMonth.get(weeksInMonth.keyAt(i)), 0));
            }
        } else if (mDataType == DataType.ALL_TIME) {
            final SparseArray<SparseArray<Double>> years =
                    groupValues(sparseArray, Calendar.YEAR, Calendar.YEAR);
            if (years.size() > 0) {
                dataSets.add(getData(0, "", years.get(years.keyAt(0)), 0));
            }
        }

        return new LineData(dataSets);
    }

    private void updateContributedTimeTitle() {
        double totalValue = 0;

        for (ILineDataSet lineDataSet : mChart.getData().getDataSets()) {
            for (int i = 0; i < lineDataSet.getEntryCount(); i++) {
                totalValue += lineDataSet.getEntryForIndex(i).getY();
            }
        }

        if (totalValue < 1) {
            mContributedTimeTv.setText(R.string.contributed_no_data);
            mChart.setVisibility(View.GONE);
        } else if (totalValue >= 1.0 && totalValue < 2.0) {
            mContributedTimeTv.setText(R.string.hours_contributed_label_singular);
            mChart.setVisibility(View.VISIBLE);
        } else {
            mContributedTimeTv.setText(getString(R.string.hours_contributed_label_plural,
                                                 (int) totalValue));
            mChart.setVisibility(View.VISIBLE);
        }
    }

    private void setLegends() {
        if (mChart.getData().getDataSets().size() > 1) {
            final Resources resources = ApplicationData.getAppContext().getResources();
            final float density = getResources().getDisplayMetrics().density;
            final float legendWidth = resources.getDimension(R.dimen.chart_legend_width) / density;
            final float legendHeight = resources.getDimension(R.dimen.chart_legend_height) / density;
            final float legendMargin = resources.getDimension(R.dimen.chart_legend_margin) / density;
            final float legendCorner = resources.getDimension(R.dimen.chart_legend_corner) / density;

            for (ILineDataSet lineDataSet : mChart.getData().getDataSets()) {
                final CheckBox checkBox = new CheckBox(mLegendLayout.getContext());
                checkBox.setChecked(true);
                checkBox.setText(lineDataSet.getLabel());
                checkBox.setTag(lineDataSet);
                checkBox.setOnCheckedChangeListener(mLegendCheckedChangeListener);

                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setColor(lineDataSet.getColor());
                drawable.setSize((int) legendWidth, (int) legendHeight);
                drawable.setCornerRadius(legendCorner);

                checkBox.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
                checkBox.setCompoundDrawablePadding((int) legendMargin);

                final GridLayout.Spec titleTxtSpecColumn = GridLayout.spec(GridLayout.UNDEFINED);
                final GridLayout.Spec titleRowSpec = GridLayout.spec(GridLayout.UNDEFINED);
                final GridLayout.LayoutParams layoutParams =
                        new GridLayout.LayoutParams(titleRowSpec, titleTxtSpecColumn);
                layoutParams.setMargins((int) legendWidth, 0, (int) legendWidth, 0);
                mLegendLayout.addView(checkBox, layoutParams);
            }
        }
    }


    public enum DataType {
        WEEK, MONTH, ALL_TIME;

        public static String getTitle(final int position) {
            return getTitle(DataType.values()[position]);
        }

        public static String getTitle(final DataType dataType) {
            switch (dataType) {
                case WEEK:
                    return ApplicationData.getAppContext().getString(R.string.week);
                case MONTH:
                    return ApplicationData.getAppContext().getString(R.string.month);
                case ALL_TIME:
                    return ApplicationData.getAppContext().getString(R.string.all_time);
                default:
                    return ApplicationData.getAppContext().getString(R.string.week);
            }
        }

        public String getTitle() {
            return getTitle(this);
        }
    }

    private static class AxisWeekValueFormatter implements IAxisValueFormatter {
        private static final String[] SHORT_WEEK_DAYS_ORIGINAL =
                DateFormatSymbols.getInstance().getShortWeekdays();
        private static final String[] SHORT_WEEK_DAYS =
                Arrays.copyOfRange(SHORT_WEEK_DAYS_ORIGINAL, 1, SHORT_WEEK_DAYS_ORIGINAL.length);

        public AxisWeekValueFormatter() {
        }

        @Override
        public String getFormattedValue(final float value, final AxisBase axis) {
            final int weekDay = (int) Math.floor(value);
            final int index = (weekDay - 1) % SHORT_WEEK_DAYS.length;
            return SHORT_WEEK_DAYS[index];
        }

        @Override
        public int getDecimalDigits() {
            return 0;
        }
    }

    private static class AxisYearValueFormatter implements IAxisValueFormatter {
        public AxisYearValueFormatter() {
        }

        @Override
        public String getFormattedValue(final float value, final AxisBase axis) {
            return String.valueOf((int) Math.floor(value));
        }

        @Override
        public int getDecimalDigits() {
            return 0;
        }
    }

    private static class AxisHoursValueFormatter implements IAxisValueFormatter {

        public AxisHoursValueFormatter() {
        }

        @Override
        public String getFormattedValue(final float value, final AxisBase axis) {
            return ApplicationData.getAppContext().getString(R.string.time_string_hours_only,
                                                             (int) Math.floor(value));
        }

        @Override
        public int getDecimalDigits() {
            return 0;
        }
    }
}
