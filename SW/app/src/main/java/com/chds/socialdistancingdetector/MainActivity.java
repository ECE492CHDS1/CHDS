package com.chds.socialdistancingdetector;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
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
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;

// Activity for pairing user's own haptic device

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private int REQUEST_SETTINGS = 2;

    public final static int CONNECTING_FRAGMENT = 1;
    public final static int SCANNING_FRAGMENT = 2;

    public static final UUID UART_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UART_RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final String HAPTIC_DEVICE_ALERT = "alert\n";

    private int fragmentStatus;

    private Handler mHandler;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    TextView statusBanner;
    String selectedDeviceAddress;

    List<Geofence> geofenceList;
    Double selectedLat;
    Double selectedLong;
    Boolean geofencingEnabled;
    PendingIntent geofencePendingIntent = null;

    public static void checkPermissions(Activity activity, Context context) {
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_PRIVILEGED,
        };

        if (!hasPermissions(context, PERMISSIONS)) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSION_ALL);
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

        statusBanner = findViewById(R.id.main_status_banner);

        geofenceList = new ArrayList<>();

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
                ScanFilter filter = new ScanFilter.Builder()
                        .setDeviceName("CHDS Haptic Device")
                        .build();
                filters.add(filter);
            }
        }

        displayFragment(CONNECTING_FRAGMENT);
    }

    public void displayFragment(int fragmentStatus) {
        this.fragmentStatus = fragmentStatus;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        getFragmentManager().popBackStack();
        switch (fragmentStatus) {
            case CONNECTING_FRAGMENT:
                statusBanner.setText("Connect to a Device");
                ConnectingFragment connectingFragment = new ConnectingFragment();
                ft.replace(R.id.fragment_layout_manager, connectingFragment);
                break;
            case SCANNING_FRAGMENT:
                statusBanner.setText("Detecting Nearby Devices");
                ScanningFragment scanningFragment = new ScanningFragment(selectedDeviceAddress);
                ft.replace(R.id.fragment_layout_manager, scanningFragment);
                break;
        }
        ft.commitAllowingStateLoss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // options menu contains button going to profile
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                // go to profile activity
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra("latitude", selectedLat);
                intent.putExtra("longitude", selectedLong);
                intent.putExtra("geofencingEnabled", geofencingEnabled);

                startActivityForResult(intent, REQUEST_SETTINGS);
//                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public Handler getmHandler() {
        return mHandler;
    }

    public BluetoothLeScanner getmLEScanner() {
        return mLEScanner;
    }

    public List<ScanFilter> getFilters() {
        return filters;
    }

    public ScanSettings getSettings() {
        return settings;
    }

    public void setSelectedDeviceAddress(String selectedDeviceAddress) {
        this.selectedDeviceAddress = selectedDeviceAddress;
    }

    public void connectToDevice(String deviceAddress) {
        if (mBluetoothAdapter == null) {
            Log.w("connectToDevice", "BluetoothAdapter not initialized.");
            return;
        }

        if (deviceAddress == null) {
            Log.w("connectToDevice", "Unspecified address.");
            return;
        }

        if (mGatt != null) {
            Log.d("connectToDevice", "Trying to use an existing mBluetoothGatt for connection.");
            mGatt.connect();
            return;
        }

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(deviceAddress);
        if (device == null) {
            Log.w("connectToDevice", "Device not found.  Unable to connect.");
            return;
        }

        if (mGatt == null) {
            Log.i("connectToDevice", "Starting Gatt Connection");
            mGatt = device.connectGatt(this, false, gattCallback);
        }

    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("deviceAddress", selectedDeviceAddress);
                    Log.w("BluetoothGattCallback", "Successfully connected to deviceAddress " + selectedDeviceAddress);
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from deviceAddress " + selectedDeviceAddress);
                    gatt.close();
                    mGatt = null;
                }
            } else {
                Toast.makeText(MainActivity.this, "Could not connect to haptic device", Toast.LENGTH_SHORT).show();
                Log.w("BluetoothGattCallback", "Error " + String.valueOf(status) + " encountered for deviceAddress: " + selectedDeviceAddress + ". Disconnecting...");
                gatt.close();
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // services are discovered
                Log.i("onServicesDiscovered", "In onServicesDiscovered");
            }
            writeRxCharacteristic(gatt, HAPTIC_DEVICE_ALERT);
            gatt.disconnect();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
        }


    };

    public void writeRxCharacteristic(BluetoothGatt gatt, String message) {
        byte[] value = message.getBytes();
        BluetoothGattService RxService = gatt.getService(UART_SERVICE_UUID);
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
        boolean status = gatt.writeCharacteristic(RxChar);

        Log.d("writeRxCharacteristic", "write RXchar - status=" + status);
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.

        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    private void setUpGeofencing() {
        geofenceList.clear();
        geofenceList.add(new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId("Geofence")

                .setCircularRegion(
                        selectedLat,
                        selectedLong,
                        500
                )
                .setExpirationDuration(-1)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());

        GeofencingRequest geofencingRequest = getGeofencingRequest();
        geofencePendingIntent = getGeofencePendingIntent();

        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        geofencingClient
                .addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("geoFencingClient", "Success");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("geoFencingClient", e.toString());
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }

        if (requestCode == REQUEST_SETTINGS) {
            if (resultCode == Activity.RESULT_CANCELED) {
                // Cancel button was pressed, do nothing
                return;
            } else {
                selectedLat = data.getDoubleExtra("selectedLat", 0);
                selectedLong = data.getDoubleExtra("selectedLong", 0);
                geofencingEnabled = data.getBooleanExtra("geofencingEnabled", false);

                if (geofencingEnabled) {
                    setUpGeofencing();
                }
            }
        }
    }

    public void enableScan() {
        if (fragmentStatus == SCANNING_FRAGMENT) {
            ScanningFragment scanningFragment = (ScanningFragment) getFragmentManager().getBackStackEntryAt(0);
            scanningFragment.enableScan();
        }
    }

    public void disableScan() {
        if (fragmentStatus == SCANNING_FRAGMENT) {
            ScanningFragment scanningFragment = (ScanningFragment) getFragmentManager().getBackStackEntryAt(0);
            scanningFragment.disableScan();
        }
    }
}