package com.chds.socialdistancingdetector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ScanResultAdapter extends ArrayAdapter<CustomScanResult> {
    private final Context context;
    private final ArrayList<CustomScanResult> scanResults;

    public ScanResultAdapter(Context context, int content, ArrayList<CustomScanResult> scanResults)
    {
        super(context, content, scanResults);
        this.context = context;
        this.scanResults = scanResults;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.content, parent, false);
        }

        CustomScanResult result = scanResults.get(position);
        String deviceName = result.getDeviceName();
        String deviceUUID = result.getDeviceAddr().toString();

        TextView textViewName = view.findViewById(R.id.content_view_device_name);
        TextView textViewUUID = view.findViewById((R.id.content_view_device_uuid));

        String printName = "Name: " + textViewName;
        String printAddr = "Address: " + textViewUUID;

        textViewName.setText(printName);
        textViewUUID.setText(printAddr);

        return view;
    }

    @Override
    public String toString() {
        return "ScanResultAdapter{" +
                "scanResults=" + scanResults +
                '}';
    }
}
