package com.example.cmate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.cmate.subviews.DoubleXLabelAxisRenderer;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.LOCATION_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 */
public class ForecastFragment extends Fragment { //implements OnChartGestureListener, OnChartValueSelectedListener {

    private LineChart mChart = null;
    ArrayList<HashMap<String, String>> mapList = null;
    String stream = null;

    private LocationManager locationManager;
    private LocationListener listener;

    public ForecastFragment() {
        // Required empty public constructor
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 10:
                //noinspection MissingPermission
                locationManager.requestLocationUpdates("gps", 5000, 0, listener);
                mChart.setNoDataText("Waiting for GPS coordinates");
                mChart.invalidate();
                break;
            default:
                break;
        }
    }

    private ArrayList<HashMap<String, String>> getAttributes(String stream) {
        String[] data = stream.split("weather"); //"wind_speed");
        ArrayList<HashMap<String, String>> list = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            if (i < 2)
                continue;
            HashMap<String, String> map = new HashMap<>();
            String[] att = data[i].split(",");
            for (int j = 0; j < att.length; j++) {
                String[] atom = att[j].split(":");
                if (atom.length == 2) {
                    String key = atom[0].replace("\"", "");
                    key = key.replace("{", "");
                    key = key.replace("[", "");
                    String value = atom[1].replace("\"", "");
                    value = value.replace("}", "");
                    value = value.replace("]", "");
                    map.put(key, value);
                }
            }
            list.add(map);
        }
        return list;
    }

    private final String degToWorldCorners(float deg) {
        final float step = 360 / 16f;
        deg += step/2;
        int id = (int) (deg / step);
        switch (id) {
            case 0:
                return "N";
            case 1:
                return "NNE";
            case 2:
                return "NE";
            case 3:
                return "ENE";
            case 4:
                return "E";
            case 5:
                return "ESE";
            case 6:
                return "SE";
            case 7:
                return "SSE";
            case 8:
                return "S";
            case 9:
                return "SSW";
            case 10:
                return "SW";
            case 11:
                return "WSW";
            case 12:
                return "W";
            case 13:
                return "WNW";
            case 14:
                return "NW";
            case 15:
                return "NNW";
            default:
                return "N";
        }
    }

    void plotChart(String stream) {
        mapList = getAttributes(stream);
        ArrayList<String> labelNames = new ArrayList<>();

        ArrayList<ILineDataSet> datasets = new ArrayList<>();
        LineDataSet set = getLineData(mapList, 1, "wind_speed", labelNames);
        set.setColor(Color.GREEN);
        datasets.add(set);
        LineDataSet set1 = getLineData(mapList, 1, "wind_gust", labelNames);
        set1.setColor(Color.RED);
        datasets.add(set1);

        LineData data = new LineData(datasets);
        mChart.setData(data);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labelNames));
        xAxis.setLabelCount(labelNames.size());

        mChart.animateY(2000);
        mChart.invalidate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_forecast, container, true);

        mChart = v.findViewById(R.id.forecastChart);
        //mChart.setOnChartGestureListener(this);
        //mChart.setOnChartValueSelectedListener(this);
        mChart.getDescription().setText("48 HOUR FORECAST");
        mChart.getDescription().setTextSize(15);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(false);

        mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTH_SIDED);
        mChart.getXAxis().setLabelRotationAngle(270);
        mChart.setExtraTopOffset(15);
        mChart.setExtraBottomOffset(10);
        //mChart.getXAxis().setLabelCount(24,true);

        OkHttpClient client = new OkHttpClient();
        String url_base = "https://api.openweathermap.org/data/2.5/onecall?lat={lat}&lon={lon}&appid={API key}";

        mChart.setXAxisRenderer(new DoubleXLabelAxisRenderer(mChart.getViewPortHandler(), mChart.getXAxis(), mChart.getTransformer(YAxis.AxisDependency.LEFT), new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int id = (int)value;
                HashMap<String, String> map = mapList.get(id);
                String strDeg = map.get("wind_deg");
                String wcorner = degToWorldCorners(Float.parseFloat(strDeg));
                return wcorner;
            }
        }));

        Button requestButton = v.findViewById(R.id.request_button);
        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (stream != null)
                    plotChart(stream);

                // first check for permissions
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                                ,10);
                    }
                    return;
                }

                //noinspection MissingPermission
                locationManager.requestLocationUpdates("gps", 5000, 0, listener);
                mChart.setNoDataText("Waiting for GPS coordinates");
                mChart.invalidate();
            }
        });

        locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);


        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (stream != null)
                    return;
                String api_key = "7e0d3bbc6be30186dac2af12cde6970f";
                String url = url_base.replace("{lat}", String.valueOf(location.getLatitude())).replace("{lon}", String.valueOf(location.getLongitude())).replace("{API key}", api_key);

                Request request = new Request.Builder().url(url).build();
                mChart.setNoDataText("Waiting for weather server");
                mChart.invalidate();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            stream = response.body().string();

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    plotChart(stream);
                                }
                            });
                        }
                    }
                });
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };


        return v;
    }

    private LineDataSet getLineData(ArrayList<HashMap<String, String>> data, int groupSize, String category, ArrayList<String> labelNames) {

        ArrayList<Entry> entryArrayList = new ArrayList<>();
        float sum = 0;

        //long time = new Date().getTime();
        //long labelTime = time;

        long labelTime = Long.parseLong(data.get(0).get("dt")) * 1000L;
        DateFormat pstFormat = new SimpleDateFormat("HH");
        pstFormat.setTimeZone(TimeZone.getDefault());

        int i=0;
        while (true) {
            if (i < data.size()) {
                float value = Float.parseFloat(data.get(i).get(category));
                sum += value;
            }
            boolean noMoreData = (i+1) >= data.size();
            if ((i+1)%groupSize == 0) {
                entryArrayList.add( new Entry(i/groupSize, sum/groupSize ) );

                Date date = new Date(labelTime);
                String dateString = pstFormat.format(date);
                labelNames.add(dateString);
                sum = 0;
                if (noMoreData)
                    break;
                try {
                    labelTime = Long.parseLong(data.get(i + 1).get("dt")) * 1000L;
                } catch (NumberFormatException e) {
                    break;
                }
            }
            i++;
        }

        LineDataSet set = new LineDataSet(entryArrayList, category);
        set.setFillAlpha(110);
        set.setLineWidth(3f);
        //set.setHighlightEnabled(true);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        //set.setValueTextSize(10f);
        return set;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause () {
        super.onPause();
    }

}
