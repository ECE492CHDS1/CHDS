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
    private static final String HAPTIC_DEVICE_ALERT = "alert";
    private BluetoothGatt mGatt;

    ListView deviceList;
    ScanResultAdapter deviceAdapter;
    ArrayList<CustomScanResult> dataList;
    HashMap<String, Integer> addrMap;
    BluetoothDevice selectedDevice;

    public static final UUID UART_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UART_RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

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

        final Button buzzButton = findViewById(R.id.send_alert);
        buzzButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                System.out.println("Alert Button Clicked!");
                writeRxCharacteristic(HAPTIC_DEVICE_ALERT);
            }
        });

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CustomScanResult result = deviceAdapter.getItem(i);
                connectToDevice(result.getDevice());

//                Intent show = new Intent(MainActivity.this, PairActivity.class);
//                show.putExtra("device", new Gson().toJson(result));
//                startActivity(show);
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
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
        }
    }


    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            Log.i("connectToDevice", "Starting Gatt Connection");
            mGatt = device.connectGatt(this, false, gattCallback);
            // TODO: Check result of createBond()
            device.createBond();
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String deviceAddress = gatt.getDevice().getAddress();
            Log.i("deviceAddress", deviceAddress);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress");
                    // TODO: Store a reference to BluetoothGatt
                    gatt.discoverServices();

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress");
                    gatt.close();
                }
            } else {
                Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...");
                gatt.close();
            }

        }

        private void printGattTable(List<BluetoothGattService> services) {
            if (services.isEmpty()) {
                Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?");
                return;
            }

            for (BluetoothGattService service : services) {
                List<BluetoothGattCharacteristic> characteristicsTable = service.getCharacteristics();
                String characteristics = "";

                for (BluetoothGattCharacteristic cr : characteristicsTable) {
                    characteristics += cr.toString() + ", ";
                }

                Log.i(
                        "printGattTable",
                        "Service: " + service.getUuid().toString() + "\nCharacteristics: " + characteristics
                );
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i("onServicesDiscovered", "In onServicesDiscovered");

            List<BluetoothGattService> services = gatt.getServices();

            printGattTable(services);

            // gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));

            // finish();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());

            // gatt.disconnect();
        }


    };

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