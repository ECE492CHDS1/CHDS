package com.chds.socialdistancingdetector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e("GeofenceReceiver", "Error" + errorMessage);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        Intent newIntent = new Intent("GeofenceReceiver");
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.i("geofenceTransition", "In geofenceTransition enter");

            newIntent.putExtra("Geofence", "Entered");
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            newIntent.putExtra("Geofence", "Exited");
        }

        context.sendBroadcast(newIntent);
    }
}
