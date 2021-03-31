package com.chds.socialdistancingdetector;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class ScanningFragment extends Fragment {
    MainActivity mainActivity;
    BluetoothGatt mGatt;
    BluetoothDevice device;

    public ScanningFragment(BluetoothGatt mGatt) {
        // Required empty public constructor
        this.mGatt = mGatt;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connecting, container, false);
        mainActivity = ((MainActivity) getActivity());

        return view;
    }
}