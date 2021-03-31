package com.chds.socialdistancingdetector;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ScanningFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ScanningFragment extends Fragment {
    MainActivity mainActivity;

    public ScanningFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connecting, container, false);
        mainActivity = ((MainActivity) getActivity());

        return view;
    }
}