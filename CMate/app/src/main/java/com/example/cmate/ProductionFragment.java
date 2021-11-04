package com.example.cmate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListPopupWindow;

import com.example.cmate.ble.SerialService;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProductionFragment extends Fragment {

    private BarChart productionChart = null;

    private Handler mHandler;
    boolean newDataAvailable = false;
    View v;

    //ArrayList<BarEntry> barEntryArrayList;
    //ArrayList<String> labelNames;
    //private ArrayList<SerialService.Category> categoryArrayList;
    private enum PowerType {WATTS, AMPS};
    private int groupSize = 1;
    private PowerType powerType = PowerType.WATTS;
    private BroadcastReceiver broadcastReceiver;

    public ProductionFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //View v = super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_production, container, true);

        productionChart = v.findViewById(R.id.production_chart);
        Description description = new Description();
        description.setText("");
        description.setTextAlign(Paint.Align.RIGHT);
        //description.setTextSize(20);
        productionChart.setDescription(description);
        //productionChart.setExtraTopOffset(80);
        XAxis xAxis = productionChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(15);
        xAxis.setLabelRotationAngle(270);


        //updateChart();


        Button powerTimeUnitButton = v.findViewById(R.id.power_time_unit);
        powerTimeUnitButton.bringToFront();
        powerTimeUnitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> timeUnitArrayList = new ArrayList<String>();
                timeUnitArrayList.add("15 min");
                timeUnitArrayList.add("30 min");
                timeUnitArrayList.add("45 min");
                timeUnitArrayList.add("hour");
                ArrayAdapter<String> timeUnitAdapter = new ArrayAdapter<String>(getContext(), R.layout.listpopupitemview);
                timeUnitAdapter.addAll(timeUnitArrayList);

                ListPopupWindow popup = new ListPopupWindow(getContext());
                popup.setAnchorView(getActivity().findViewById(R.id.power_time_unit));
                popup.setWidth(300); //measureContentWidth(context, adapter)
                popup.setHeight(ListPopupWindow.WRAP_CONTENT);
                popup.setModal(true);
                popup.setAdapter(timeUnitAdapter);

                popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String value = parent.getAdapter().getItem(position).toString();
                        ((Button)getActivity().findViewById(R.id.power_time_unit)).setText("per " + value);
                        groupSize = position+1;
                        updateChart();
                        popup.dismiss();
                        productionChart.animateY(2000);
                        productionChart.invalidate();
                    }
                });
                popup.show();
            }
        });

        Button powerUnitButton = v.findViewById(R.id.power_unit);
        powerUnitButton.bringToFront();
        powerUnitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> timeUnitArrayList = new ArrayList<String>();
                timeUnitArrayList.add("watts");
                timeUnitArrayList.add("amps");
                ArrayAdapter<String> unitAdapter = new ArrayAdapter<String>(getContext(), R.layout.listpopupitemview);
                unitAdapter.addAll(timeUnitArrayList);

                ListPopupWindow popup = new ListPopupWindow(getContext());
                popup.setAnchorView(getActivity().findViewById(R.id.power_unit));
                popup.setWidth(300); //measureContentWidth(context, adapter)
                popup.setHeight(ListPopupWindow.WRAP_CONTENT);
                popup.setModal(true);
                popup.setAdapter(unitAdapter);

                popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String value = parent.getAdapter().getItem(position).toString();
                        ((Button)getActivity().findViewById(R.id.power_unit)).setText(value);
                        powerType = (position==0) ? PowerType.WATTS : PowerType.AMPS;
                        updateChart();
                        popup.dismiss();
                        productionChart.animateY(2000);
                        productionChart.invalidate();
                    }
                });
                popup.show();
            }
        });


        //if (((MainActivity)getActivity()).serialService != null)
        updateChart();

        schedulePoll();

        return v;
    }

    // Update chart by polling
    void schedulePoll() {
        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (newDataAvailable) {
                    newDataAvailable = false;
                    updateChart();
                }
                schedulePoll();
            }
        }, 60000);
    }

    //boolean firstTime = true;
    void updateChart() {
        boolean animate = (productionChart.getBarData() == null);

        /*if (firstTime)
            firstTime = false;
        else
            createDummyData();*/

        ArrayList<String> labelNames = new ArrayList<>();
        BarData barData = getBarData(groupSize, powerType, labelNames);
        if (labelNames.isEmpty()) {
            Date date = new Date();
            String dateString = (new SimpleDateFormat("dd-MM-hh:mm")).format(date) + "_";
            labelNames.add(dateString);
            animate = false;
        }

        //if (barData.getDataSetCount() == 0)
        //    return;



        productionChart.setData(barData);

        XAxis xAxis = productionChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labelNames));
        xAxis.setLabelCount(labelNames.size());

        if (animate) {
            productionChart.animateY(2000);
        }
        productionChart.invalidate();
    }

    private BarData getBarData(int groupSize, PowerType powerType, ArrayList<String> labelNames) {
        ArrayList<SerialService.Category> categoryArrayList;
        if (((MainActivity) getActivity()).serialService != null)
            categoryArrayList = ((MainActivity) getActivity()).serialService.getCategoryArrayList();
        else
            return null;

        if (categoryArrayList.isEmpty())
            return new BarData();

        ArrayList<BarEntry> barEntryArrayList = new ArrayList<>();
        float sum = 0;

        SerialService.Category labelCategory = categoryArrayList.get(0);
        int i=0;
        while (true) {
            if (i < categoryArrayList.size()) {
                SerialService.Category category = categoryArrayList.get(i);
                sum += (powerType == PowerType.WATTS) ? category.watt : category.amp;
            }
            boolean noMoreData = (i+1) >= categoryArrayList.size();
            if ((i+1)%groupSize == 0) {
                barEntryArrayList.add( new BarEntry(i/groupSize, sum / (labelCategory.minutesSpan*groupSize)) );
                Date date = new Date(labelCategory.time);
                String dateString = (new SimpleDateFormat("dd-MM-hh:mm")).format(date);
                labelNames.add(dateString);
                sum = 0;
                if (noMoreData)
                    break;
                labelCategory = categoryArrayList.get(i+1);
            }
            i++;
        }

        BarDataSet barDataSet = new BarDataSet(barEntryArrayList, "");
        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        barDataSet.setValueTextSize(10);
        return new BarData(barDataSet);
    }

    @Override
    public void onResume() {
        super.onResume();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (SerialService.ACTION_CONNECTED.equals(action)) {
                    updateChart();
                } else if (SerialService.ACTION_POWER.equals(action)) {
                    newDataAvailable = true;
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SerialService.ACTION_CONNECTED);
        intentFilter.addAction(SerialService.ACTION_POWER);
        getActivity().registerReceiver(broadcastReceiver, intentFilter);

        newDataAvailable = false;
        updateChart();
    }

    @Override
    public void onPause () {
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

}