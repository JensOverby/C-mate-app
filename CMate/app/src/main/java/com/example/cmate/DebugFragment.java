package com.example.cmate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.example.cmate.ble.SerialService;
import java.io.IOException;

/**
 * A simple {@link Fragment} subclass.
 */
public class DebugFragment extends Fragment {

    private final static String TAG = DebugFragment.class.getSimpleName();
    private EditText sendMsg = null;
    private EditText recvMsg = null;
    private BroadcastReceiver broadcastReceiver;

    public DebugFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_debug, container, false);

        Button sendButton = (Button) v.findViewById(R.id.button_send_msg);
        sendMsg = (EditText) v.findViewById(R.id.et_tx);
        recvMsg = (EditText) v.findViewById(R.id.received_txt);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ((MainActivity)getActivity()).serialService.write((sendMsg.getText().toString() + "\r\n").getBytes());
                }
                catch (IOException e) {
                    System.out.println("not connected");
                }
                sendMsg.setText(null);
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (SerialService.ACTION_DATA.equals(action)) {
                    Log.d(TAG, "receive data");
                    byte[] value = intent.getByteArrayExtra(SerialService.EXTRA_DATA);
                    recvMsg.append(new String(value));
                    if(value != null){
                        Log.d(TAG, new String(value));
                    }
                    else {
                        Log.d(TAG, "value = null");
                    }
                }
                else if (SerialService.ACTION_CONNECTED.equals(action)) {
                    System.out.println("BroadcastReceiver: "+"cmate connected");
                }
                else if(SerialService.ACTION_SERIAL_ERROR.equals(action)) {
                    System.out.println("BroadcastReceiver: "+"device disconnected");
                }
                else {
                    Log.d(TAG, "unknown data received");
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SerialService.ACTION_CONNECTED);
        intentFilter.addAction(SerialService.ACTION_DATA);
        intentFilter.addAction(SerialService.ACTION_SERIAL_ERROR);
        getActivity().registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onPause () {
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

}
