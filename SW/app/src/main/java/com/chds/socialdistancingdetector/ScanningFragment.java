package com.chds.socialdistancingdetector;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class ScanningFragment extends Fragment {
    MainActivity mainActivity;
    BluetoothLeScanner mLEScanner;
    List<ScanFilter> filters;
    ScanSettings settings;
    Handler scanHandler;
    Boolean scanEnabled;

    String connectedDeviceAddress;
    private static final long SCAN_PERIOD = 5000;
    private static final Integer TX_POWER_1M = -55;

    HashMap<String, CustomScanResult> scanResultHashMap;

    Button backButton;

    public ScanningFragment(String deviceAddress) {
        // Required empty public constructor
        connectedDeviceAddress = deviceAddress;
        Log.i("ScanningFragment", "deviceAddress set:" + deviceAddress);
        scanResultHashMap = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scanning, container, false);
        mainActivity = ((MainActivity) getActivity());
        mLEScanner = mainActivity.getmLEScanner();
        scanHandler = new Handler();
        filters = mainActivity.getFilters();
        settings = mainActivity.getSettings();

        backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                disableScan();
                mainActivity.displayFragment(MainActivity.CONNECTING_FRAGMENT);
            }
        });

        // start scanning
        enableScan();

        return view;
    }


    private void measureDistance() {
        Log.i("Distance measure", "Starting");
        boolean sendAlert = false;
        HashSet<String> removeSet = new HashSet<>();

        for (CustomScanResult result : scanResultHashMap.values()) {
            if (result.getRawRssiValues().size() == 0) {
                removeSet.add(result.getDeviceAddr());
                continue;
            }

            double rssiValue = result.computeRssiValue();

            Log.i("Distance measure", "Final RSSI value: " + rssiValue);

            double distance = Math.pow(10, (TX_POWER_1M - rssiValue) / (10 * 2));

            Log.i("Distance measure", "Distance measured: " + distance);

            if (distance <= 2) {
                sendAlert = true;
            }
        }

        scanResultHashMap.keySet().removeAll(removeSet);

        if (sendAlert) {
            mainActivity.connectToDevice(connectedDeviceAddress);
        }

        Log.i("Distance measure", "Done");
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        private void addScanResultToArray(ScanResult result) {
            String address = result.getDevice().getAddress();
            if (address.equals(connectedDeviceAddress)) {
                return;
            }

            if (scanResultHashMap.containsKey(address)) {
                CustomScanResult existingResult = scanResultHashMap.get(address);
                existingResult.addRawRssiValue(result.getRssi());
                scanResultHashMap.put(address, existingResult);
                Log.i("Scan", "Scan result: " + existingResult.toString());
            } else {
                CustomScanResult newResult = new CustomScanResult(result);
                scanResultHashMap.put(address, newResult);
                Log.i("Scan", "Scan result: " + newResult.toString());
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResultToArray(result);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i("Scan", "Batch scan results:" + scanResultHashMap.toString());
            for (ScanResult result : results) {
                addScanResultToArray(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan", "Failed! Error Code: " + errorCode);
        }
    };

    public void enableScan() {
        scanEnabled = true;
        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                // stop scanning
                Log.i("Scan", "Stopping");
                mLEScanner.stopScan(mScanCallback);

                if (scanEnabled) {
                    measureDistance();

                    // start scanning once again
                    Log.i("Scan", "Starting");
                    mLEScanner.startScan(filters, settings, mScanCallback);

                    // 'this' is referencing the Runnable object
                    scanHandler.postDelayed(this, SCAN_PERIOD);
                }
            }
        };

        // start scanning for the first time
        Log.i("Scan", "Starting");
        mLEScanner.startScan(filters, settings, mScanCallback);
        scanHandler.postDelayed(runnableCode, SCAN_PERIOD);
    }

    public void disableScan() {
        scanEnabled = false;
        Log.i("Scan", "Disabled");
        scanHandler.removeCallbacksAndMessages(null);
        mLEScanner.stopScan(mScanCallback);
    }
}