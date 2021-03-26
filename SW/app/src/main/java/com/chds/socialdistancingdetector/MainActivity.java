package com.chds.socialdistancingdetector;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;

// Activity for pairing user's own haptic device

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private int REQUEST_PAIR_REQUEST = 2;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 3000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;


    ListView deviceList;
    ScanResultAdapter deviceAdapter;
    ArrayList<CustomScanResult> dataList;
    HashMap<String, Integer> addrMap;
    BluetoothDevice selectedDevice;

    public static void checkPermissions(Activity activity, Context context){
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_PRIVILEGED,
        };

        if(!hasPermissions(context, PERMISSIONS)) {
            ActivityCompat.requestPermissions( activity, PERMISSIONS, PERMISSION_ALL);
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceList = findViewById(R.id.device_list);
        dataList = new ArrayList<>();
        addrMap = new HashMap<String, Integer>();
        deviceAdapter = new ScanResultAdapter(this, R.layout.content, dataList);

        deviceList.setAdapter(deviceAdapter);

        final Button scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                System.out.println("Scan Button Clicked!");
                scanLeDevice(true);
            }
        });

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CustomScanResult result = deviceAdapter.getItem(i);
                Intent show = new Intent(MainActivity.this, PairActivity.class);
                show.putExtra("device", new Gson().toJson(result));
                startActivity(show);
            }
        });

        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        checkPermissions(MainActivity.this, this);

        // todo: Check if app's paired device is cached. If so, move to "scan/dist calc activity"

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                        .setReportDelay(10000)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        } else if (requestCode == REQUEST_PAIR_REQUEST) {
            if (resultCode == Activity.RESULT_CANCELED) {
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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