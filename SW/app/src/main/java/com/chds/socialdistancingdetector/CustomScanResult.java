package com.chds.socialdistancingdetector;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

import static java.sql.Types.NULL;

public class CustomScanResult implements Serializable
{
    private String deviceName;
    private String deviceAddr;
    private BluetoothDevice device;
    private ArrayList<Integer> rawRssiValues;
    private double rssiValue = 0;
    private int mtxPower;
    private static double FILTER_COEFFICIENT = 0.75;

    public CustomScanResult(ScanResult result)
    {
        deviceName = result.getDevice().getName();
        deviceAddr = result.getDevice().getAddress();
        device = result.getDevice();
        rawRssiValues = new ArrayList<Integer>();
        rawRssiValues.add(result.getRssi());
        mtxPower = result.getScanRecord().getTxPowerLevel();
    }

    public CustomScanResult()
    {

    }

    public String getDeviceName()
    {
        return deviceName;
    }

    public ArrayList<Integer> getRawRssiValues() {
        return rawRssiValues;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public String getDeviceAddr() {
        return deviceAddr;
    }

    public int getMtxPower() {
        return mtxPower;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setDeviceAddr(String deviceAddr) {
        this.deviceAddr = deviceAddr;
    }

    public void addRawRssiValue(Integer rssiValue) {
        this.rawRssiValues.add(rssiValue);
    }

    public double getRssiValue() { return this.rssiValue; }

    public double computeRssiValue() {
        double sum = 0;
        for (Integer rssiValue : rawRssiValues) {
            sum += rssiValue;
        }

        if (rssiValue == 0) {
            rssiValue = sum / rawRssiValues.size();
        } else {
            rssiValue = rssiValue * (1 - FILTER_COEFFICIENT) + FILTER_COEFFICIENT * (sum / rawRssiValues.size());
        }

        rawRssiValues.clear();
        return rssiValue;
    }

    @Override
    public String toString() {
        return "CustomScanResult{" +
                "deviceName='" + deviceName + '\'' +
                ", deviceAddr=" + deviceAddr +
                ", device=" + device +
                ", rssiValues=" + rawRssiValues +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomScanResult that = (CustomScanResult) o;
        return Objects.equals(deviceAddr, that.deviceAddr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceAddr);
    }
}
