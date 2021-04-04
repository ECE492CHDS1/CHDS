package com.chds.socialdistancingdetector;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class ScanningFragment extends Fragment {
    MainActivity mainActivity;
    BluetoothGatt mGatt;
    String connectedDeviceAddress;
    private static final long SCAN_PERIOD = 1000;
    public static final UUID UART_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UART_RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final String HAPTIC_DEVICE_ALERT = "alert";
    private static final Integer TX_POWER_1M = -55;

    HashMap<String, CustomScanResult> scanResultHashMap;

    public ScanningFragment(String deviceAddress, BluetoothGatt mGatt) {
        // Required empty public constructor
        connectedDeviceAddress = deviceAddress;

        this.mGatt = mGatt;
        scanResultHashMap = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scanning, container, false);
        mainActivity = ((MainActivity) getActivity());

        Handler handler = new Handler();

        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                // Do something here on the main thread
                scanLeDevice(true);
                measureDistance();

                Log.i("Algorithm", "Time to implement it");
                
                // 'this' is referencing the Runnable object
                handler.post(this);
            }
        };


        handler.post(runnableCode);
        return view;
    }


    private void measureDistance() {
        Log.i("distance measure", "Starting");
        Collection<CustomScanResult> scanResults = scanResultHashMap.values();

        for (CustomScanResult result : scanResults) {
            if (result.getRawRssiValues().size() == 0) {
                scanResultHashMap.remove(result.getDeviceAddr());
                continue;
            }

            double rssiValue = result.computeRssiValue();

            Log.i("distance measure", "Final RSSI value: " + rssiValue);

            double distance = Math.pow(10, (TX_POWER_1M - rssiValue) / (10 * 2));

            Log.i("distance measure", "Distance measured: " + distance);

            if (distance <= 2) {
                writeRxCharacteristic(HAPTIC_DEVICE_ALERT);
                return;
            }
        }
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
                Log.i("scan", "result: " + existingResult.toString());
            } else {
                CustomScanResult newResult = new CustomScanResult(result);
                scanResultHashMap.put(address, newResult);
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResultToArray(result);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i("batchScan Results", scanResultHashMap.toString());
            for (ScanResult result : results) {
                addScanResultToArray(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private void scanLeDevice(final boolean enable) {
        Handler mHandler = mainActivity.getmHandler();
        BluetoothLeScanner mLEScanner = mainActivity.getmLEScanner();
        List<ScanFilter> filters = mainActivity.getFilters();
        ScanSettings settings = mainActivity.getSettings();

        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);

            mLEScanner.startScan(filters, settings, mScanCallback);
            Log.i("scanLeDevice", "Start LeScan with mScanCallback");

        } else {
            mLEScanner.stopScan(mScanCallback);
        }
    }

    public void writeRxCharacteristic(String message) {
        byte[] value = message.getBytes();
        BluetoothGattService RxService = mGatt.getService(UART_SERVICE_UUID);
        if (RxService == null) {
            Log.i("writeRxCharacteristic", "RxService not found");
            return;
        }
        BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(UART_RX_CHAR_UUID);

        if (RxChar == null) {
            Log.i("writeRxCharacteristic", "RxChar not found");
            return;
        }

        RxChar.setValue(value);
        boolean status = mGatt.writeCharacteristic(RxChar);

        Log.d("writeRxCharacteristic", "write TXchar - status=" + status);

    }

}