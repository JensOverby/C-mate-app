package com.example.cmate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.cmate.ble.SerialService;
import com.example.cmate.subviews.BatteryView;
import com.example.cmate.subviews.SimpleGaugeView;

import java.util.ArrayList;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 */
public class ControlsFragment extends Fragment {

    private SimpleGaugeView rpm = null;
    private SimpleGaugeView v_in = null;
    private SimpleGaugeView watts = null;
    private SimpleGaugeView amp = null;
    private SimpleGaugeView v_out = null;
    private BatteryView bv = null;
    private BroadcastReceiver broadcastReceiver;
    //Date mDate = new Date();

    public ControlsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_controls, container, false);
        rpm = (SimpleGaugeView)v.findViewById(R.id.rpm_view);
        v_in = (SimpleGaugeView)v.findViewById(R.id.volts_in_view);
        watts = (SimpleGaugeView)v.findViewById(R.id.watts_view);
        amp = (SimpleGaugeView)v.findViewById(R.id.amps_out_view);
        v_out = (SimpleGaugeView)v.findViewById(R.id.volts_out_view);
        bv = (BatteryView)v.findViewById(R.id.battery_view);

        return v;
    }


    /*double getBatteryLevel(double v, boolean charging) {
        if (charging) {
            double vv = v*v;
            return 12.5 - 0.026 * v + 2.26E-03 * vv - 4.04E-05 * vv * v + 2.32E-07 * vv * vv;
        }
        return 7.73E-03 * v + 11.9;
    }*/

    @Override
    public void onResume() {
        super.onResume();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (SerialService.ACTION_CONNECT_ERROR.equals(action) || SerialService.ACTION_SERIAL_ERROR.equals(action)) {
                }
                else if (SerialService.ACTION_POWER.equals(action)) {
                    byte[] data = intent.getByteArrayExtra(SerialService.EXTRA_POWER);
                    String str = new String(data);
                    if (str.matches("du(.*)")) {
                        String[] str1 = str.split("[a-zA-Z ]+");
                        if (str1.length==8) {
                            v_in.setValue(Float.parseFloat(str1[2]));
                            v_out.setValue(Float.parseFloat(str1[3]));
                            amp.setValue(Float.parseFloat(str1[4]));
                            watts.setValue(Float.parseFloat(str1[5]));
                            rpm.setValue(Integer.parseInt(str1[7]));

                            SerialService serialService = ((MainActivity) context).serialService;
                            bv.setPercent(serialService.getBatteryLevelPct());
                            bv.setIsCharging(serialService.isBatteryCharging());
                        }
                    }
                }
                else if (SerialService.ACTION_SAMPLE.equals(action)) {
                    byte[] data = intent.getByteArrayExtra(SerialService.EXTRA_SAMPLE);
                    String str = new String(data);
                    String[] str1 = str.split("[a-zA-Z_= ]+");
                    if (str1.length==4) {
                        v_in.setValue(Float.parseFloat(str1[1]));
                        v_out.setValue(Float.parseFloat(str1[3]));
                        bv.setIsCharging(false);
                        bv.setPercent(((MainActivity) context).serialService.getBatteryLevelPct());
                    }
                }
                else if (SerialService.ACTION_SPINDOWN.equals(action)) {
                    v_in.setValue(0);
                    rpm.setValue(0);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SerialService.ACTION_CONNECT_ERROR);
        intentFilter.addAction(SerialService.ACTION_SERIAL_ERROR);
        intentFilter.addAction(SerialService.ACTION_POWER);
        intentFilter.addAction(SerialService.ACTION_SAMPLE);
        intentFilter.addAction(SerialService.ACTION_SPINUP);
        intentFilter.addAction(SerialService.ACTION_SPINDOWN);
        getActivity().registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onPause () {
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }
}
