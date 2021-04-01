package com.chds.socialdistancingdetector;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class CustomScanResult implements Serializable
{
    private String deviceName;
    private String deviceAddr;
    private BluetoothDevice device;
    private ArrayList<Integer> rssiValues;
    private int mtxPower;

    public CustomScanResult(ScanResult result)
    {
        deviceName = result.getDevice().getName();
        deviceAddr = result.getDevice().getAddress();
        device = result.getDevice();
        rssiValues = new ArrayList<Integer>();
        rssiValues.add(result.getRssi());
        mtxPower = result.getScanRecord().getTxPowerLevel();
    }

    public CustomScanResult()
    {

    }

    public String getDeviceName()
    {
        return deviceName;
    }

    public ArrayList<Integer> getRssiValues() {
        return rssiValues;
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

    public void addRssiValue(Integer rssiValue) {
        this.rssiValues.add(rssiValue);
    }

    @Override
    public String toString() {
        return "CustomScanResult{" +
                "deviceName='" + deviceName + '\'' +
                ", deviceAddr=" + deviceAddr +
                ", device=" + device +
                ", rssiValues=" + rssiValues +
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
