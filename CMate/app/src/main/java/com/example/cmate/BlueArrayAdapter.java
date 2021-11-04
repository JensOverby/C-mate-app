package com.example.cmate;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

// Adapter for holding devices found through scanning

public class BlueArrayAdapter extends ArrayAdapter<String> {

    private final static String TAG = BlueArrayAdapter.class.getSimpleName();

    public ArrayList<BluetoothDevice> mLeDevices;
    ScanCallBackImpl scanCallBack;
    private int errorCode;
    private static final int OS_VERSION_LOLLIPOP = 21;
    private Activity mActivity;

    public BlueArrayAdapter(Activity activity, int resource) {
        super(activity, resource);
        mActivity = activity;
        mLeDevices = new ArrayList<BluetoothDevice>();
    }

    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public String getItem(int i) {
        String name = mLeDevices.get(i).getName();
        String addr = mLeDevices.get(i).getAddress();
        if (name != null)
            return name + " " + addr;
        return addr;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void scanLeDevice(final boolean enable) {
        if (Build.VERSION.SDK_INT < OS_VERSION_LOLLIPOP) {
            Log.i(TAG, "NOT_SUPPORTED");
            return;
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.i(TAG, "NOT_SUPPORTED");
            return;
        }
        BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            Log.i(TAG, "NOT_SUPPORTED");
            return;
        }

        if (enable) {
            clear();
            try {
                scanCallBack = new ScanCallBackImpl();
                ScanSettings.Builder builder = new ScanSettings.Builder();
                builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                builder.setReportDelay(0);
                bluetoothLeScanner.startScan(null, builder.build(), scanCallBack);
            } catch (Exception e) {
                Log.i(TAG, "startLeScan was not successful!");
            }
        } else {
            bluetoothLeScanner.flushPendingScanResults(scanCallBack);
            bluetoothLeScanner.stopScan(scanCallBack);
        }
        //getActivity().invalidateOptionsMenu();
    }

    private class ScanCallBackImpl extends ScanCallback {

        @Override
        public void onScanFailed(int errorCode) {
            BlueArrayAdapter.this.errorCode = errorCode;
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            boolean devicesAdded = false;
            for (ScanResult result : results) {
                BluetoothDevice device = result.getDevice();
                if (mLeDevices.contains(device))
                    continue;
                mLeDevices.add(device);
                devicesAdded = true;
            }
            if (!devicesAdded)
                return;

            mActivity.runOnUiThread(() -> notifyDataSetChanged());
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (mLeDevices.contains(device))
                return;
            mLeDevices.add(device);

            mActivity.runOnUiThread(() -> notifyDataSetChanged());
        }
    }
}
