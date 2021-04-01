package com.chds.socialdistancingdetector;

import android.bluetooth.BluetoothDevice;
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

import java.util.ArrayList;
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
    BluetoothDevice device;
    private static final long SCAN_PERIOD = 3000;
    public static final UUID UART_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UART_RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

    ArrayList<CustomScanResult> dataList;
    HashMap<String, Integer> addrMap;

    public ScanningFragment(BluetoothGatt mGatt) {
        // Required empty public constructor
        this.mGatt = mGatt;
        addrMap = new HashMap<>();
        this.dataList = new ArrayList<>();
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

                Log.i("Algorithm", "Time to implement it");

                // Repeat this the same runnable code block again another 2 seconds
                // 'this' is referencing the Runnable object
                handler.postDelayed(this, 5000);
            }
        };


        handler.post(runnableCode);
        return view;
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        private void addScanResultToArray(ScanResult result) {
            String tempAddr = result.getDevice().getAddress();
            CustomScanResult tempResult = new CustomScanResult(result);

            if (!dataList.isEmpty() && addrMap.containsKey(tempAddr)) {
                CustomScanResult existingResult = dataList.get(addrMap.get(tempAddr));
                existingResult.addRssiValue(result.getRssi());
                dataList.set(addrMap.get(tempAddr), existingResult);
            } else {
                dataList.add(tempResult);
                addrMap.put(tempAddr, dataList.size()-1);
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResultToArray(result);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i("batchScan Results", dataList.toString());
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

            dataList.clear();
            addrMap.clear();

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