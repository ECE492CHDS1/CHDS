package com.chds.socialdistancingdetector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {
    Switch geofenceSwitch;
    Button setButton;
    Button cancelButton;
    Button saveButton;

    Location selectedLocation;

    LocationManager locationManager;

    private Location getCurrentLocation() {
        if (
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED)
        {
            String[] PERMISSIONS = {
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

            ActivityCompat.requestPermissions (
                    SettingsActivity.this,
                    PERMISSIONS,
                    1
            );

            return null;
        }

        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        geofenceSwitch = findViewById(R.id.geofence_switch);
        setButton = findViewById(R.id.set_geofence_button);
        cancelButton = findViewById(R.id.cancel_button);
        saveButton = findViewById(R.id.save_button);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        Boolean isEnabled = getIntent().getBooleanExtra("geofencingEnabled", false);
        geofenceSwitch.setChecked(isEnabled);

        Double selectedLat = getIntent().getDoubleExtra("latitude",  -360);
        Double selectedLong = getIntent().getDoubleExtra("longitude", -360);

        if (selectedLat != -360 && selectedLong != -360) {
            selectedLocation = new Location(LocationManager.GPS_PROVIDER);
            selectedLocation.setLatitude(selectedLat);
            selectedLocation.setLongitude(selectedLong);
        }

        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedLocation = getCurrentLocation();
                Toast.makeText(SettingsActivity.this, "Successfully selected current location", Toast.LENGTH_SHORT).show();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent output = new Intent();

                if (geofenceSwitch.isChecked() && selectedLocation == null) {
                    selectedLocation = getCurrentLocation();
                }

                if (geofenceSwitch.isChecked()) {
                    output.putExtra("selectedLat", selectedLocation.getLatitude());
                    output.putExtra("selectedLong", selectedLocation.getLongitude());
                }

                output.putExtra("geofencingEnabled", geofenceSwitch.isChecked());
                setResult(RESULT_OK, output);
                finish();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

}