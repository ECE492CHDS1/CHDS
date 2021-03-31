package com.chds.socialdistancingdetector;

import android.app.Activity;
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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the  factory method to
 * create an instance of this fragment.
 */
public class ConnectingFragment extends Fragment {
    ListView deviceList;
    ScanResultAdapter deviceAdapter;
    ArrayList<CustomScanResult> dataList;
    HashMap<String, Integer> addrMap;
    private static final long SCAN_PERIOD = 3000;
    MainActivity mainActivity;

    public ConnectingFragment() {
        // Required empty public constructor
        dataList = new ArrayList<>();
        addrMap = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_connecting, container, false);
        mainActivity = ((MainActivity) getActivity());

        Log.i("connectingFragment", "Reached Connecting Fragment");

        dataList = new ArrayList<>();

        deviceList = view.findViewById(R.id.device_list);
        deviceAdapter = new ScanResultAdapter(getContext(), R.layout.content, dataList);
        deviceList.setAdapter(deviceAdapter);

        final Button scanButton = view.findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("ScanButton", "Scan Button Clicked!");
                scanLeDevice(true);
            }
        });

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CustomScanResult result = deviceAdapter.getItem(i);
                mainActivity.connectToDevice(result.getDevice());
            }
        });

        // Inflate the layout for this fragment
        return view;
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
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

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
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

            Log.i("batchScan Results", dataList.toString());
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
                    deviceAdapter.notifyDataSetChanged();
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
}