package com.example.cmate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.cmate.ble.SerialService;
import com.example.cmate.ble.SerialSocket;
import com.example.cmate.subviews.PowerView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Semaphore;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private final static String TAG = MainActivity.class.getSimpleName();
    public SerialService serialService = null;
    public ViewPager2 viewPager = null;
    private ImageView imageViewTail;
    private ImageView imageViewWings;
    private ImageView imageBattery;
    private ImageView imageCharging;
    private TextView batteryLevelText;

    private SpinnerAnimator spinner;
    private TailAnimator tail;
    private ProgressBar batteryStatusProgressBar;

    private Button connectionButton;

    private BlueArrayAdapter mBlueArrayAdapter = null;
    //private BluetoothDevice mDevice = null;
    private String mDeviceAddress = null;
    private static final long SCAN_PERIOD = 10000;
    private Handler mHandler;

    private enum ConState {SCAN, SCANNING, CONNECTING, DISCONNECT}; // SCAN_COMPLETED,
    private ConState mState = ConState.SCAN;
    private PowerView powerView;
    private BroadcastReceiver broadcastReceiver;

    boolean isRestoreInstanceState = false;

    private class KeepAliveRunnable implements Runnable {
        MainActivity mActivity;
        KeepAliveRunnable (MainActivity activity) {
            mActivity = activity;
        }
        @Override
        public void run() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                unbindService(mActivity);
            } catch (Exception ignored) {
            }
            mActivity.serialService = null;
        }
    }

    @Override
    protected void onSaveInstanceState(@NotNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        //outState.putBoolean("has_service", serialService != null);

        if (serialService != null && mDeviceAddress != null) {
            outState.putString("device_address", mDeviceAddress);

            // Stops scanning after a pre-defined scan period.
            unregisterReceiver(broadcastReceiver);
            KeepAliveRunnable keepAliveRunnable = new KeepAliveRunnable(this);
            new Thread(keepAliveRunnable).start();
        }
        // Save the state of item position
        //outState.putInt(SELECTED_ITEM_POSITION, mPosition);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        serialService = null;
        if (mDeviceAddress != null) {
            isRestoreInstanceState = true;
            boolean bound = bindService(new Intent(this, SerialService.class), this, Context.BIND_AUTO_CREATE);
            if (!bound)
                Log.d(TAG, "Binding service failed!");
        }
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mDeviceAddress = savedInstanceState.getString("device_address");
        if (mDeviceAddress != null) {
            isRestoreInstanceState = true;
            boolean bound = bindService(new Intent(this, SerialService.class), this, Context.BIND_AUTO_CREATE);
            if (!bound)
                Log.d(TAG, "Binding service failed!");
        }

        // Read the state of item position
        //mPosition = savedInstanceState.gettInt(SELECTED_ITEM_POSITION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        batteryStatusProgressBar = (ProgressBar) findViewById(R.id.battery_status);
        batteryStatusProgressBar.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
        imageBattery = (ImageView) findViewById(R.id.batteryView);
        imageCharging = (ImageView) findViewById(R.id.chargingView);
        batteryLevelText = findViewById(R.id.battery_level_txt);
        batteryLevelText.setVisibility(View.INVISIBLE);

        imageViewTail = (ImageView) findViewById(R.id.dashView);
        tail = new TailAnimator(imageViewTail);
        imageViewWings = (ImageView) findViewById(R.id.wingsView);
        imageViewWings.setVisibility(View.INVISIBLE);
        TextView rpmText = findViewById(R.id.rpm_txt);
        spinner = new SpinnerAnimator(imageViewWings, rpmText, this);

        mBlueArrayAdapter = new BlueArrayAdapter(this, R.layout.listpopupitemview);
        connectionButton = (Button) findViewById(R.id.connection_button);
        connectionButton.setOnClickListener(v1 -> { startScan(); });
        mHandler = new Handler();

        powerView = new PowerView(this, R.id.power_frame);

        viewPager = findViewById(R.id.swipe);
        //viewPager.setUserInputEnabled(false);
        PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(pagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabBar);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Log");
                    break;
                case 1:
                    tab.setText("Live");
                    break;
                case 2:
                    tab.setText("Forecast");
                    break;
                case 3:
                    tab.setText("Debug");
                    break;
            }
        }).attach();
        /*tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });*/

        setState(mState);
    }

    /*int counter = 0;
    public void handleAnimation(View view) {
        if (counter == 0) {
            spinner.setRpm(110, true);
            counter = 1;
        }
        else {
            spinner.setRpm(0, true);
            counter = 0;
        }
        //spinner.counter++;
    }*/


    private ConState getState() {
        return mState;
    }

    public void setState(ConState state) {
        switch (state) {
            case SCAN:
                connectionButton.setText("Offline");
                imageViewWings.setVisibility(View.INVISIBLE);
                connectionButton.setEnabled(true);
                connectionButton.setVisibility(View.VISIBLE);
                break;
            case SCANNING:
                connectionButton.setEnabled(false);
                connectionButton.setVisibility(View.INVISIBLE);
                ListPopupWindow popup = createListPopupWindow();
                popup.setAdapter(mBlueArrayAdapter);
                popup.show();
                break;
            case CONNECTING:
                connectionButton.setEnabled(false);
                connectionButton.setText("...");
                connectionButton.setVisibility(View.VISIBLE);
                break;
            case DISCONNECT:
                connectionButton.setText("Online");
                imageViewWings.setVisibility(View.VISIBLE);
                connectionButton.setVisibility(View.VISIBLE);
                connectionButton.setEnabled(true);
                break;
            default:
                break;
        }

        mState = state;
    }

    private ListPopupWindow createListPopupWindow() {
        ListPopupWindow bleList = new ListPopupWindow(this);
        bleList.setAnchorView(connectionButton);
        bleList.setWidth(800); //measureContentWidth(context, adapter)
        bleList.setHeight(ListPopupWindow.WRAP_CONTENT);
        bleList.setModal(true);

        bleList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /*if (getState() == ConState.SCAN_COMPLETED && mBlueArrayAdapter.isRescanPosition(position)) {
                    startScan();
                    bleList.dismiss();
                    return;
                }*/

                BluetoothDevice newDevice = mBlueArrayAdapter.getDevice(position);
                Log.d(TAG, "Selected: " + newDevice.getAddress());

                // Never happens:
                if (getState() == ConState.DISCONNECT && newDevice.getAddress() == mDeviceAddress)
                    return;

                mDeviceAddress = newDevice.getAddress();

                if (getState() == ConState.SCANNING)
                    mBlueArrayAdapter.scanLeDevice(false);

                setState(ConState.CONNECTING);

                boolean bound = bindService(new Intent(MainActivity.this, SerialService.class), MainActivity.this, Context.BIND_AUTO_CREATE);
                if (!bound)
                    Log.d(TAG, "Binding service failed!");

                bleList.dismiss();
            }
        });

        bleList.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (getState()== ConState.SCANNING) // || getState()==ConState.SCAN_COMPLETED)
                    setState(ConState.SCAN);
            }
        });

        return bleList;
    }

    //@SuppressLint("StaticFieldLeak") // AsyncTask needs reference to this fragment
    private void startScan() {

        if (getState()== ConState.DISCONNECT) {
            unregisterReceiver(broadcastReceiver);
            serialService.disconnect();
            try { unbindService(this); } catch(Exception ignored) {}
            setState(ConState.SCAN);
            serialService = null;
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.location_permission_title);
                builder.setMessage(R.string.location_permission_message);
                builder.setPositiveButton(android.R.string.ok,
                        (dialog, which) -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0));
                builder.show();
                return;
            }
        }

        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getState()== ConState.SCANNING)
                    mBlueArrayAdapter.scanLeDevice(false);
                if (mBlueArrayAdapter.getCount() == 0)
                    setState(ConState.SCAN);
            }
        }, SCAN_PERIOD);

        mBlueArrayAdapter.scanLeDevice(true);
        setState(ConState.SCANNING);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // ignore requestCode as there is only one in this fragment
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            new Handler(Looper.getMainLooper()).postDelayed(this::startScan,1); // run after onResume to avoid wrong empty-text
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getText(R.string.location_denied_title));
            builder.setMessage(getText(R.string.location_denied_message));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.show();
        }
    }

    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mDeviceAddress);//.getAddress());
            System.out.println("connecting...");
            SerialSocket socket = new SerialSocket(getApplicationContext(), device);
            serialService.connect(socket);
        } catch (Exception e) {
            System.out.println("connection failed...");
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        serialService = ((SerialService.SerialBinder) binder).getService();

        if (serialService.isConnected()) {
            if (isRestoreInstanceState) {
                mState = ConState.DISCONNECT;
                setState(mState);
                serialService.mainActivityRecreated();
            }
        }
        else {
            runOnUiThread(this::connect);
        }
        isRestoreInstanceState = false;

        /*if (!isRestoreInstanceState)
            runOnUiThread(this::connect);
        else {
            mState = (serialService.isConnected())? ConState.DISCONNECT : ConState.SCAN;
            setState(mState);
            serialService.mainActivityRecreated();
        }
        isRestoreInstanceState = false;*/

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SerialService.ACTION_CONNECTED);
        intentFilter.addAction(SerialService.ACTION_DISCONNECT);
        intentFilter.addAction(SerialService.ACTION_CONNECT_ERROR);
        intentFilter.addAction(SerialService.ACTION_SERIAL_ERROR);
        intentFilter.addAction(SerialService.ACTION_SPINUP);
        intentFilter.addAction(SerialService.ACTION_POWER);
        intentFilter.addAction(SerialService.ACTION_SAMPLE);
        intentFilter.addAction(SerialService.ACTION_SPINDOWN);
        intentFilter.addAction(SerialService.ACTION_WIND);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "end Service Connection");
        serialService = null;
    }



    private int batteryColorFromPct(int pct) {
        int val = (pct > 50) ? 2 * (pct - 50) : 0;
        int red = (val > 50) ? 2 * (50 - (val-50)) : 100;
        int green = (val > 50) ? 100 : 2*val;
        int color = 0xC0000000 + (int)(2.55*red) * 0x10000 + (int)(2.55*green) * 0x100;
        return color;
    }

    @Override
    protected void onResume() {
        super.onResume();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (SerialService.ACTION_POWER.equals(action)) {
                    byte[] data = intent.getByteArrayExtra(SerialService.EXTRA_POWER);
                    String str = new String(data);
                    System.out.println("debugk: " + str);
                    if (str.matches("du(.*)")) {
                        String[] str1 = str.split("[a-zA-Z ]+");
                        if (str1.length == 8) {
                            float rpm = Integer.parseInt(str1[7]);
                            spinner.setRpm(rpm, false);
                            if (tail.gustLevel > 0)
                                tail.setGustLevel(0);
                            int pct = serialService.getBatteryLevelPct();
                            int color = batteryColorFromPct(pct);

                            batteryStatusProgressBar.setProgress(pct);
                            batteryStatusProgressBar.setProgressTintList(ColorStateList.valueOf(color));
                            batteryStatusProgressBar.invalidate();
                            if (serialService.isBatteryCharging()) {
                                if (imageCharging.getVisibility() == View.INVISIBLE) {
                                    imageCharging.setVisibility(View.VISIBLE);
                                    imageCharging.invalidate();
                                    batteryLevelText.setVisibility(View.INVISIBLE);
                                    batteryLevelText.invalidate();
                                }
                            } else {
                                if (imageCharging.getVisibility() == View.VISIBLE) {
                                    imageCharging.setVisibility(View.INVISIBLE);
                                    imageCharging.invalidate();
                                    batteryLevelText.setText(Integer.toString(pct)+"%");
                                    batteryLevelText.setVisibility(View.VISIBLE);
                                    batteryLevelText.invalidate();
                                }
                            }

                            float watts = Float.parseFloat(str1[5]);
                            powerView.setValue(watts);
                            System.out.println("here now!");
                        }
                    }
                } else if (SerialService.ACTION_WIND.equals(action)) {
                    byte[] data = intent.getByteArrayExtra(SerialService.EXTRA_WIND);
                    String str = new String(data);
                    if (str.matches("acc(.*)")) {
                        String[] str1 = str.split("[a-zA-Z =]+");
                        if (str1.length == 4) {
                            float gustLevel = Integer.parseInt(str1[3]) + Float.parseFloat(str1[1]) / Float.parseFloat(str1[2]);
                            tail.setGustLevel(gustLevel);
                        }
                    }
                    //spinner.setRpm(0, false);
                    //spinner.assureInvisible();
                } else if (SerialService.ACTION_SAMPLE.equals(action)) {
                    System.out.println(Looper.myLooper());
                    System.out.println(Looper.getMainLooper());

                    int levelPct = serialService.getBatteryLevelPct();
                    int color = batteryColorFromPct(levelPct);
                    batteryStatusProgressBar.setProgressTintList(ColorStateList.valueOf(color));
                    batteryStatusProgressBar.setProgress(levelPct);
                    batteryLevelText.setText(Integer.toString(levelPct) + "%");
                    batteryLevelText.setVisibility(View.VISIBLE);
                    if (imageCharging.getVisibility() == View.VISIBLE) {
                        imageCharging.setVisibility(View.INVISIBLE);
                        imageCharging.invalidate();
                    }
                    //spinner.assureInvisible();
                } else if (SerialService.ACTION_CONNECTED.equals(action)) {
                    setState(ConState.DISCONNECT);
                } else if (SerialService.ACTION_DISCONNECT.equals(action)) {
                    System.out.println("service disconnecting");
                } else if (SerialService.ACTION_SPINUP.equals(action)) {
                    System.out.println("spinup received!");
                    tail.setGustLevel(0);
                    spinner.setRpm(120, true);
                } else if (SerialService.ACTION_SPINDOWN.equals(action)) {
                    spinner.setRpm(0, false);
                    //spinner.assureInvisible();
                } else if (SerialService.ACTION_CONNECT_ERROR.equals(action)) {
                    Log.d(TAG, "ACTION_CONNECT_ERROR");
                    serialService.disconnect();
                    try {
                        unbindService(MainActivity.this);
                    } catch (Exception ignored) {
                    }
                    setState(ConState.SCAN);
                } else if (SerialService.ACTION_SERIAL_ERROR.equals(action)) {
                    Log.d(TAG, "ACTION_SERIAL_ERROR");
                    serialService.disconnect();
                    try {
                        unbindService(MainActivity.this);
                    } catch (Exception ignored) {
                    }
                    setState(ConState.SCAN);
                } else {
                    Log.d(TAG, "Should not be possible to be here!");
                    return;
                }
            }
        };
    }

    /*protected void onPause() {
        unregisterReceiver(broadcastReceiver);
        try { unbindService(this); } catch(Exception ignored) {}
        super.onPause();
    }*/
}
