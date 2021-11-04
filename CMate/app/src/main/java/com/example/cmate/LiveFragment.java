package com.example.cmate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.cmate.ble.SerialService;
import com.example.cmate.subviews.MySwipeButton;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * A simple {@link Fragment} subclass.
 */
public class LiveFragment extends Fragment {

    private BroadcastReceiver broadcastReceiver;

    MySwipeButton spinupButton;
    MySwipeButton initWindButton;
    MySwipeButton turnOnButton;
    MySwipeButton brakeButton;

    private long lastTime = 0;

    //private void setZero(float[] a, int i) { for (int j=0; j<i; j++) {a[j] = 0;} }

    private Random random = new Random();
    /*private void addNoise(float[] a, int i) {
        for (int j=0; j<i; j++) {
            a[j] = (float)random.nextGaussian()/4; // random.nextInt(100) / 500.f - 0.1f;
        }
    }*/

    private float[] createEmptyFloatArray(int i) {float[] a = new float[4]; for (int j=0; j<i; j++) {a[j] = 0;} return a;}
    //private int[] createEmptyIntArray(int i) {int[] a = new int[4]; for (int j=0; j<i; j++) {a[j] = 0;} return a;}

    private final int chartCount = 3;
    private float[] v = createEmptyFloatArray(chartCount);
    private float[] s = createEmptyFloatArray(chartCount);
    private float[] s_target = createEmptyFloatArray(chartCount);
    private final MyLineChart[] chart = new MyLineChart[chartCount-1];

    private PowerRunnable powerRunnable;

    private volatile boolean stopThread = true;
    private volatile boolean threadIsRunning = false;
    private volatile float interval = 1;

    public LiveFragment() {
        // Required empty public constructor
    }

    private void setChartProperties(LineChart chart, String description) {
        chart.getDescription().setEnabled(true);
        chart.getDescription().setText(description);
        chart.getDescription().setTextSize(15);
        chart.setTouchEnabled(false);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setPinchZoom(false);
        chart.setBackgroundColor(Color.WHITE);
        chart.setDrawBorders(false);
        //chart.setVisibleYRangeMinimum(2, YAxis.AxisDependency.LEFT);
        chart.setAutoScaleMinMaxEnabled(true);
        //chart.setMaxVisibleValueCount(40);

        //chart.setVisibleYRangeMinimum(5f, YAxis.AxisDependency.LEFT);
        //chart.setVisibleYRangeMinimum(5f, YAxis.AxisDependency.RIGHT);
        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);//.WHITE);
        data.setDrawValues(false);
        chart.setData(data);

        Legend l = chart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.BLACK);

        XAxis xl = chart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(false);
        xl.setEnabled(true);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(true);
        leftAxis.setEnabled(true);
        //leftAxis.setAxisMaximum(10f);
        //leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setTextColor(Color.BLACK);
        rightAxis.setDrawGridLines(true);
        rightAxis.setEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_live, container, false);

        chart[0] = v.findViewById(R.id.power_chart_view);
        setChartProperties(chart[0], "POWER");
        chart[0].setMyMinScale(5);
        /*XAxis xAxis = chart[0].getXAxis();
        ArrayList<String> labelNames = new ArrayList<>();
        labelNames.add("WATTS");
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labelNames));
        xAxis.setLabelCount(labelNames.size());*/

        chart[1] = v.findViewById(R.id.voltages_chart_view);
        setChartProperties(chart[1], "VOLTAGES");
        /*xAxis = chart[1].getXAxis();
        labelNames = new ArrayList<>();
        labelNames.add("VOLTAGE IN");
        labelNames.add("VOLTAGE OUT");
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labelNames));
        xAxis.setLabelCount(labelNames.size());*/

        /*chart[2] = v.findViewById(R.id.voltage_in_chart_view);
        setChartProperties(chart[2], "voltage in", Color.MAGENTA);

        chart[3] = v.findViewById(R.id.voltage_out_chart_view);
        setChartProperties(chart[3], "voltage out", Color.MAGENTA);*/

        spinupButton = (MySwipeButton) v.findViewById(R.id.spinup_swipe);
        spinupButton.setOnActiveListener(new MySwipeButton.OnActiveListener() {
            @Override
            public void onActive() {
                try {
                    SerialService service = ((MainActivity)getActivity()).serialService;
                    if (service != null) {
                        //Toast.makeText(getActivity(), "Forced Spinup", Toast.LENGTH_SHORT).show();
                        service.write(("spinup\r\n").getBytes());
                    }
                    else
                        Toast.makeText(getActivity(), "Not connected!", Toast.LENGTH_SHORT).show();
                }
                catch (IOException e) {
                    System.out.println("not connected");
                }
            }
        });

        initWindButton = (MySwipeButton) v.findViewById(R.id.init_wind_swipe);
        initWindButton.setOnActiveListener(new MySwipeButton.OnActiveListener() {
            @Override
            public void onActive() {
                ((MainActivity)getActivity()).viewPager.setUserInputEnabled(true);
                try {
                    SerialService service = ((MainActivity)getActivity()).serialService;
                    if (service != null) {
                        initWindButton.setText("sampling\n...");
                        initWindButton.setBackgroundResource(R.drawable.shape_rounded_red);
                        initWindButton.setEnabled(false);
                        initWindButton.user=1;
                        service.write(("initstartwind\r\n").getBytes());
                        Toast.makeText(getActivity(), "Now sampling wind speed", Toast.LENGTH_SHORT).show();
                    }
                    else
                        Toast.makeText(getActivity(), "Not connected!", Toast.LENGTH_SHORT).show();
                }
                catch (IOException e) {
                    System.out.println("not connected");
                }
            }
        });

        turnOnButton = (MySwipeButton) v.findViewById(R.id.turn_on_swipe);
        turnOnButton.setOnActiveListener(new MySwipeButton.OnActiveListener() {
            @Override
            public void onActive() {
                ((MainActivity)getActivity()).viewPager.setUserInputEnabled(true);
                try {
                    SerialService service = ((MainActivity)getActivity()).serialService;
                    if (service != null) {
                        if (turnOnButton.user==0) {
                            turnOnButton.setBackgroundResource(R.drawable.shape_rounded_green);
                            turnOnButton.setText("automatic\nmode");
                            turnOnButton.user=2;
                            Toast.makeText(getActivity(), "Activating automatic wind sensor spinup", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            turnOnButton.setBackgroundResource(R.drawable.shape_rounded_red);
                            turnOnButton.setText("manual\nmode");
                            turnOnButton.user=0;
                            Toast.makeText(getActivity(), "Activating manual mode spinup", Toast.LENGTH_SHORT).show();
                        }
                        service.write(("ack\r\n").getBytes());
                    }
                    else
                        Toast.makeText(getActivity(), "Not connected!", Toast.LENGTH_SHORT).show();
                }
                catch (IOException e) {
                    System.out.println("not connected");
                }
            }
        });

        brakeButton = (MySwipeButton) v.findViewById(R.id.stop_swipe);
        brakeButton.setOnActiveListener(new MySwipeButton.OnActiveListener() {
            @Override
            public void onActive() {
                ((MainActivity)getActivity()).viewPager.setUserInputEnabled(true);
                try {
                    SerialService service = ((MainActivity)getActivity()).serialService;
                    if (service != null) {
                        service.write(("stop\r\n").getBytes());
                        Toast.makeText(getActivity(), "Immediate BRAKE!", Toast.LENGTH_SHORT).show();
                    }
                    else
                        Toast.makeText(getActivity(), "Not connected!", Toast.LENGTH_SHORT).show();
                }
                catch (IOException e) {
                    System.out.println("not connected");
                }
            }
        });

        return v;
    }

    private MyILineDataSet createSet(int color, String label) {
        MyILineDataSet set = new MyILineDataSet(null, label);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(4f);
        set.setColor(color);//Color.MAGENTA);
        set.setHighlightEnabled(true);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.LINEAR);
        //set.setCubicIntensity(0.2f);
        return set;
    }

    private class PowerRunnable implements Runnable {
        private final int sleeptime = 251; // prime number
        private final int countDownFrom = 3000/sleeptime;
        private long tLast = 0;
        byte countDown = 0;

        void keepAlive() { countDown = countDownFrom; }

        @Override
        public void run() {
            tLast = (new Date()).getTime() - 100;
            stopThread = false;
            threadIsRunning = true;
            while (true) {
                if (stopThread) {
                    threadIsRunning = false;
                    return;
                }

                long nowTime = (new Date()).getTime();
                float dt = ((nowTime-tLast)/1000f)/interval;
                tLast = nowTime;

                for (int i=0; i<chartCount; i++) {
                    if (v[i] != 0) {
                        s[i] += v[i] * dt;
                        if (s[i] > s_target[i]) {
                            if (v[i] > 0) {
                                s[i] = s_target[i];
                                v[i] = 0;
                            }
                        } else {
                            if (s[i] < 0) {
                                s[i] = 0;
                                v[i] = 0;
                            } else if (v[i] < 0) {
                                s[i] = s_target[i];
                                v[i] = 0;
                            }
                        }
                    }

                    MyLineChart ch = chart[(i==0) ? 0 : 1];
                    LineData data = ch.getData();
                    if (data != null)
                    {
                        MyILineDataSet set = (MyILineDataSet) data.getDataSetByIndex((i==0) ? 0 : i-1);
                        if (set == null) {
                            switch (i) {
                                case 0:
                                    set = createSet(Color.RED, "WATTS");
                                    break;
                                case 1:
                                    set = createSet(Color.BLUE, "VOLTAGE IN");
                                    break;
                                case 2:
                                    set = createSet(Color.MAGENTA, "VOLTAGE OUT");
                                    break;
                                default:
                                    //set = createSet(Color.MAGENTA);
                                    break;
                            }
                            data.addDataSet(set);
                        }

                        if (countDown == 0) {
                            v[i] = 0;
                            s[i] = s_target[i];
                            s[i] += (float) (random.nextGaussian() / 50);
                        }

                        set.setEntry(s[i], 120);

                        if (i == 0 || i==2) {
                            data.notifyDataChanged();

                            final Semaphore mutex = new Semaphore(0);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ch.notifyDataSetChanged();
                                    ch.invalidate();
                                    mutex.release();
                                }
                            });
                            try {
                                mutex.acquire();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                countDown--;
                if (countDown < 0) {
                    countDown = 0;

                    s_target[0] = 0; // rpm
                    //s_target[1] = 0; // watts
                    try {
                        Thread.sleep(1999);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        Thread.sleep(sleeptime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void setValues(float[] value) {
        long nowTime = (new Date()).getTime();
        interval = (nowTime-lastTime) / 1000f;
        if (interval > 3 || interval < 0.5)
            interval = 2;
        lastTime = nowTime;

        for (int i=0; i<chartCount; i++) {
            s_target[i] = value[i];

            if (s_target[i] > 0) {
                float dS = s_target[i] - s[i];
                v[i] = dS / interval;
            }
        }

        //chart[0].setAutoScaleMinMaxEnabled(false);
        //chart[1].setAutoScaleMinMaxEnabled(false);
    }

    private void setVoltages(float voltage_in, float voltage_out) {
        lastTime = (new Date()).getTime();

        s_target[1] = voltage_in;
        s_target[2] = voltage_out;
        //chart[0].setAutoScaleMinMaxEnabled(true);
        //chart[1].setAutoScaleMinMaxEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        powerRunnable = new PowerRunnable();

        if (((MainActivity)getActivity()).serialService != null) {
            if ( ((MainActivity)getActivity()).serialService.isConnected() ) {
                Thread thread = new Thread(powerRunnable);
                //thread.setPriority(2);
                thread.start();
            }
        }

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (!threadIsRunning) {
                    if (((MainActivity) getActivity()).serialService != null) {
                        if (((MainActivity) getActivity()).serialService.isConnected()) {
                            new Thread(powerRunnable).start();
                        }
                    }
                }

                final String action = intent.getAction();
                if (SerialService.ACTION_CONNECTED.equals(action)) {
                }
                else if (SerialService.ACTION_DISCONNECT.equals(action) || SerialService.ACTION_CONNECT_ERROR.equals(action) || SerialService.ACTION_SERIAL_ERROR.equals(action)) {
                    stopThread = true;
                }
                else if (SerialService.ACTION_POWER.equals(action)) {
                    byte[] data = intent.getByteArrayExtra(SerialService.EXTRA_POWER);
                    String str = new String(data);
                    if (str.matches("du(.*)")) {
                        String[] str1 = str.split("[a-zA-Z ]+");
                        if (str1.length==8) {
                            float[] value = new float[chartCount];
                            value[0] = Float.parseFloat(str1[5]); // watts
                            value[1] = Float.parseFloat(str1[2]); // voltage in
                            value[2] = Float.parseFloat(str1[3]); // voltage out
                            setValues(value);
                            if (Integer.parseInt(str1[7]) > 0)
                                powerRunnable.keepAlive();
                            System.out.println("LiveFragment here!");
                        }
                    }
                }
                else if (SerialService.ACTION_SAMPLE.equals(action)) {
                    byte[] data = intent.getByteArrayExtra(SerialService.EXTRA_SAMPLE);
                    String str = new String(data);
                    String[] str1 = str.split("[a-zA-Z_= ]+");
                    if (str1.length==4) {
                        setVoltages(Float.parseFloat(str1[1]), Float.parseFloat(str1[3]));
                    }
                }
                else if (SerialService.ACTION_SPINUP.equals(action)) {
                    //chart[0].setAutoScaleMinMaxEnabled(false);
                    //chart[1].setAutoScaleMinMaxEnabled(false);
                }
                else if (SerialService.ACTION_SPINDOWN.equals(action)) {
                }
                else if (SerialService.ACTION_WIND.equals(action)) {
                    if (turnOnButton.user == 0) {
                        turnOnButton.setBackgroundResource(R.drawable.shape_rounded_green);
                        turnOnButton.setText("automatic\nmode");
                        turnOnButton.user = 1;
                    }
                    if (initWindButton.user == 1) {
                        initWindButton.setText("swipe to\ninit sensor");
                        initWindButton.setBackgroundResource(R.drawable.shape_rounded);
                        initWindButton.setEnabled(true);
                        initWindButton.user = 0;
                    }
                }
                else if (SerialService.ACTION_STOPPED.equals(action)) {
                    if (turnOnButton.user==1) {
                        turnOnButton.setBackgroundResource(R.drawable.shape_rounded_red);
                        turnOnButton.setText("manual\nmode");
                        turnOnButton.user=0;
                    }
                    else if (turnOnButton.user == 2)
                        turnOnButton.user = 1;

                    if (initWindButton.user == 1) {
                        initWindButton.setText("swipe to\ninit sensor");
                        initWindButton.setBackgroundResource(R.drawable.shape_rounded);
                        initWindButton.setEnabled(true);
                        initWindButton.user = 0;
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SerialService.ACTION_WIND);
        intentFilter.addAction(SerialService.ACTION_CONNECTED);
        intentFilter.addAction(SerialService.ACTION_MAINACTIVITY_BACK);
        intentFilter.addAction(SerialService.ACTION_CONNECT_ERROR);
        intentFilter.addAction(SerialService.ACTION_SERIAL_ERROR);
        intentFilter.addAction(SerialService.ACTION_POWER);
        intentFilter.addAction(SerialService.ACTION_SAMPLE);
        intentFilter.addAction(SerialService.ACTION_SPINUP);
        intentFilter.addAction(SerialService.ACTION_SPINDOWN);
        intentFilter.addAction(SerialService.ACTION_DISCONNECT);
        intentFilter.addAction(SerialService.ACTION_STOPPED);
        getActivity().registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onPause () {
        stopThread = true;
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }
}
