package com.example.cmate.ble;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.example.cmate.MainActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * create notification and queue serial data while activity is not in the foreground
 * use listener chain: SerialSocket -> SerialService -> UI fragment
 */

public class SerialService extends Service implements SerialListener {

    public final static String ACTION_CONNECTED = "com.example.cmate.ble.ACTION_CONNECTED";
    public final static String ACTION_MAINACTIVITY_BACK = "com.example.cmate.ble.ACTION_MAINACTIVITY_BACK";
    public final static String ACTION_CONNECT_ERROR = "com.example.cmate.ble.ACTION_CONNECT_ERROR";
    public final static String ACTION_DATA = "com.example.cmate.ble.ACTION_DATA";
    public final static String ACTION_SERIAL_ERROR = "com.example.cmate.ble.ACTION_SERIAL_ERROR";
    public final static String EXTRA_DATA = "com.example.cmate.ble.EXTRA_DATA";

    private long batteryVoltageTime = 0;
    private float batteryVoltage = 0;
    private int batteryLevelPct = 50;
    private boolean batteryCharging = false;
    private ArrayList<Double> chargeCurve = new ArrayList<Double>();
    private ArrayList<Double> nochargeCurve = new ArrayList<Double>();
    private void addTo(ArrayList<Double> a, double x, double y) {a.add(x); a.add(y);}

    public int getBatteryLevelPct() { return batteryLevelPct; }
    public boolean isBatteryCharging() { return batteryCharging; }

    long tPower = 0;
    private ArrayList<Category> categoryArrayList = new ArrayList<Category>();

    private static final Pattern sPattern = Pattern.compile("^([1-9][0-9]{0,2})?(\\.[0-9]?)?$");

    public class SerialBinder extends Binder {
        public SerialService getService() { return SerialService.this; }
    }

    private final IBinder binder;

    private SerialSocket socket;
    //private SerialListener listener;
    private boolean connected;

    public static class Category {
        public long time;
        public float watt;
        public float amp;
        public int minutesSpan;
    }

    public ArrayList<Category> getCategoryArrayList() { return categoryArrayList; };

    /**
     * Lifecylce
     */
    public SerialService() {
        binder = new SerialBinder();

        addTo(chargeCurve, 15.25, 100);
        addTo(chargeCurve, 14.15, 90);
        addTo(chargeCurve, 13.7, 80);
        addTo(chargeCurve, 13.4, 70);
        addTo(chargeCurve, 13.3, 60);
        addTo(chargeCurve, 13.25, 50);
        addTo(chargeCurve, 13.05, 40);
        addTo(chargeCurve, 12.8, 30);
        addTo(chargeCurve, 12.6, 20);
        addTo(chargeCurve, 12.4, 10);

        addTo(nochargeCurve, 12.8, 100);
        addTo(nochargeCurve, 12.6, 75);
        addTo(nochargeCurve, 12.37, 50);
        addTo(nochargeCurve, 12.18, 25);
        addTo(nochargeCurve, 12, 0);
    }

    @Override
    public void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public boolean isConnected() { return connected; }

    /**
     * Api
     */
    public void connect(SerialSocket socket) throws IOException {
        socket.connect(this);
        this.socket = socket;
        connected = true;
    }

    public void disconnect() {
        sendBroadcast(new Intent(ACTION_DISCONNECT));
        connected = false;
        if(socket != null) {
            socket.disconnect();
            socket = null;
        }
    }

    public void write(byte[] data) throws IOException {
        if(!connected)
            throw new IOException("not connected");
        socket.write(data);
    }

    /**
     * SerialListener
     */
    public void onSerialConnect() {
        if(connected) {
            Intent intent = new Intent(ACTION_CONNECTED);
            sendBroadcast(intent);
        }
    }

    public void mainActivityRecreated() {
        Intent intent = new Intent(ACTION_MAINACTIVITY_BACK);
        sendBroadcast(intent);
    }

    public void onSerialConnectError(Exception e) {
        if(connected) {
            Intent intent = new Intent(ACTION_CONNECT_ERROR);
            sendBroadcast(intent);
        }
    }

    public final static String ACTION_WIND = "com.example.cmate.ble.ACTION_WIND";
    public final static String ACTION_POWER = "com.example.cmate.ble.ACTION_POWER";
    public final static String ACTION_SPINUP = "com.example.cmate.ble.ACTION_SPINUP";
    public final static String ACTION_SPINDOWN = "com.example.cmate.ble.ACTION_SPINDOWN";
    public final static String ACTION_SAMPLE = "com.example.cmate.ble.ACTION_SAMPLE";
    public final static String ACTION_STOPPED = "com.example.cmate.ble.ACTION_STOPPED";
    public final static String ACTION_STARTED = "com.example.cmate.ble.ACTION_STARTED";
    public final static String ACTION_DISCONNECT = "com.example.cmate.ble.ACTION_DISCONNECT";

    public final static String EXTRA_WIND = "com.example.cmate.ble.EXTRA_WIND";
    public final static String EXTRA_POWER = "com.example.cmate.ble.EXTRA_POWER";
    public final static String EXTRA_SAMPLE = "com.example.cmate.ble.EXTRA_SAMPLE";

    //Date mDate = new Date();

    void broad(String str, String strExtra, byte[] data) {
        Intent intent = new Intent(str);
        intent.putExtra(strExtra, data);
        sendBroadcast(intent);
    }

    /*public void setSpinDown() {
        Intent intent = new Intent(ACTION_SPINDOWN);
        sendBroadcast(intent);
    }*/

    Category getCurrentCategory(long t_now, int minutesSpan) {
        int index = categoryArrayList.size() - 1;
        if (index >= 0) {
            Category currentCategory = categoryArrayList.get(index);
            if (t_now < (currentCategory.time + currentCategory.minutesSpan*60000))
                return currentCategory;
        }

        Date d_now = new Date(t_now);
        int minutes = d_now.getMinutes();
        d_now.setMinutes((minutes/minutesSpan)*minutesSpan);
        Category currentDateCategory = new Category();
        currentDateCategory.time = d_now.getTime();
        currentDateCategory.minutesSpan = minutesSpan;
        currentDateCategory.amp = 0;
        currentDateCategory.watt = 0;
        categoryArrayList.add(currentDateCategory);
        return currentDateCategory;
    }

    double interpolate(double x0, double y0, double x1, double y1, double x) { return y0 + (x-x0)/(x1-x0) * (y1-y0); }

    double getInterpolatedY(double x, ArrayList<Double> a)
    {
        int sz = a.size();
        int i = sz/4;
        if (x > a.get(0))
            return a.get(1);
        if (x <= a.get(sz-2))
            return a.get(sz-1);
        while (a.get((i+1)*2) > x)
            i++;
        while (a.get(i*2) <= x)
            i--;
        double y = interpolate(a.get(i*2), a.get(i*2+1), a.get((i+1)*2), a.get((i+1)*2+1), x);
        return y;
    }

    long sampleTime = 0;
    long stoppedTime = 0;
    boolean stopped = true;
    boolean rpm_zero = true;
    boolean spinup = false;
    int spinup_error_count = 0;
    String cmd = "";
    long lastIntegrationTime = 0;

    public void onSerialRead(byte[] data) {
        if(connected) {
            /*String str = new String("Java String Methods");

            System.out.print("Regex: (.*)String(.*) matches string? " );
            System.out.println(str.matches("(.*)String(.*)"));

            System.out.print("Regex: (.*)Strings(.*) matches string? " );
            System.out.println(str.matches("(.*)Strings(.*)"));

            System.out.print("Regex: (.*)Methods matches string? " );
            System.out.println(str.matches("(.*)Methods"));*/

            String str = new String(data);

            if (str.matches("(.*)\r\n(.*)")) {
                String[] str1 = str.split("\r\n");
                str = cmd;
                if (str1.length > 0)
                    str += str1[0];
                if (str1.length > 2) {
                    cmd = str1[1];
                    System.out.println("ss: LENGTH > 2");
                }
                else if (str1.length > 1)
                    cmd = str1[1];
                else
                    cmd = "";

                //System.out.println("dsafsd");
                //str = (cmd + str1[0]).replaceFirst("\r\n", "");
                //cmd = "";
            }
            else {
                cmd += str;
                return;
            }
            System.out.println("ss: " + str);

            long t = (new Date()).getTime();

            if (str.matches("(.*)SPI(.*)")) { // SPINUP
                rpm_zero = false;
                spinup = true;
                spinup_error_count = 0;
                sendBroadcast(new Intent(ACTION_SPINUP));
            }
            else if (str.matches("acc(.*)")) {
                broad(ACTION_WIND, EXTRA_WIND, str.getBytes());

                if (t > (sampleTime+60000)) {
                    sampleTime = t;
                    try {write("sample\r\n".getBytes());} catch (IOException e) {System.out.println(e.toString());}
                }

            }
            else if (str.matches("du(.*)")) {
                String[] str1 = str.split("[a-zA-Z ]+");
                if (str1.length==8) {

                    // watt and amp integration
                    float amp = Float.parseFloat(str1[4]);
                    float watt = Float.parseFloat(str1[5]);
                    Category category = getCurrentCategory(t, 15);
                    float dt = (t - lastIntegrationTime) / 1000.f;
                    lastIntegrationTime = t;
                    if (dt < 5) {
                        category.watt += dt * watt / 60;
                        category.amp += dt * amp / 60;
                    }

                    // Interpret battery voltage during charging
                    float voltage_out = Float.parseFloat(str1[3]);
                    batteryCharging = watt > 20;

                    if (batteryCharging) {
                        if ((t - batteryVoltageTime) > 2000) {
                            if (voltage_out > batteryVoltage)
                                batteryVoltage += 0.1;
                        }
                        batteryLevelPct = (int) getInterpolatedY(batteryVoltage, chargeCurve);
                    }
                    else {
                        batteryLevelPct = (int) getInterpolatedY(batteryVoltage, nochargeCurve);
                        batteryVoltage = voltage_out;
                        batteryVoltageTime = t;
                    }

                    /*if ((t - batteryVoltageTime) > 30000) {
                        if (batteryVoltageTime==0) {
                            batteryVoltage = voltage_out;
                            batteryLevelPct = (int)getInterpolatedY(batteryVoltage, nochargeCurve);
                        }
                        else {
                            float diff = voltage_out - batteryVoltage;
                            if (diff > 0.1f)
                                diff = 0.1f;
                            batteryVoltage += diff;
                            batteryLevelPct = (int)getInterpolatedY(batteryVoltage, chargeCurve);
                        }
                        batteryVoltageTime = t;
                    }
                    if (voltage_out < batteryVoltage) {
                        batteryVoltage = voltage_out;
                        batteryVoltageTime = t;
                        if (batteryCharging)
                            batteryLevelPct = (int)getInterpolatedY(batteryVoltage, chargeCurve);
                        else
                            batteryLevelPct = (int)getInterpolatedY(batteryVoltage, nochargeCurve);
                    }*/

                    String modStr = null;
                    boolean pass_on = true;
                    if (watt < 1) {
                        int rpm = Integer.parseInt(str1[7]);

                        if (spinup) {
                            if (rpm >= 80 && rpm <= 200) {
                                spinup = false;
                            }
                            else {
                                pass_on = false;
                                spinup_error_count++;
                                if (spinup_error_count > 2) {
                                    spinup = false;
                                    pass_on = true;
                                    modStr = str.replaceFirst("rpm[0-9]*", "rpm0");
                                }
                            }
                        }
                        else {
                            if (rpm_zero || rpm < 80 || rpm > 200) {
                                rpm_zero = true;
                                modStr = str.replaceFirst("rpm[0-9]*", "rpm0");
                            }
                        }
                    }
                    else
                        rpm_zero = false;
                    if (pass_on) {
                        if (modStr != null)
                            broad(ACTION_POWER, EXTRA_POWER, modStr.getBytes());
                        else
                            broad(ACTION_POWER, EXTRA_POWER, str.getBytes());
                    }
                }
            }
            else if (str.matches("Sta(.*)")) {
                if (str.matches("State: STOPPED(.*)")) { // SPINDOWN
                    rpm_zero = false;
                    spinup = true;
                    spinup_error_count = 0;
                    sendBroadcast(new Intent(ACTION_SPINDOWN));
                }
            }
            else if (str.matches("INI(.*)")) { // STOPPED
                if (!stopped) {
                    stopped = true;

                    if (t > (sampleTime+15000)) {
                        sampleTime = t;
                        try {write("sample\r\n".getBytes());} catch (IOException e) {System.out.println(e.toString());}
                    }
                }
                sendBroadcast(new Intent(ACTION_STOPPED));
            }
            else if (str.matches("V_in(.*)")) {
                String[] str1 = str.split("[a-zA-Z_= ]+");
                if (str1.length==4) {
                    batteryVoltage = Float.parseFloat(str1[3]);
                    batteryLevelPct = (int)getInterpolatedY(batteryVoltage, nochargeCurve);
                    batteryCharging = false;
                }
                broad(ACTION_SAMPLE, EXTRA_SAMPLE, str.getBytes());
                try {write("\r\n".getBytes());} catch (IOException e) {System.out.println(e.toString());}
            }

            if (stopped && t > (stoppedTime+10000)) {
                stopped = false;
                sendBroadcast(new Intent(ACTION_STARTED));
            }

            Intent intent = new Intent(ACTION_DATA);
            intent.putExtra(EXTRA_DATA, (str + "\r\n").getBytes());
            sendBroadcast(intent);
        }
    }


    public void onSerialIoError(Exception e) {
        if(connected) {
            Intent intent = new Intent(ACTION_SERIAL_ERROR);
            sendBroadcast(intent);
        }
    }

}
