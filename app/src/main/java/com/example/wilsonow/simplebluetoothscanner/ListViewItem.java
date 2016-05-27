package com.example.wilsonow.simplebluetoothscanner;

import android.bluetooth.BluetoothDevice;

public class ListViewItem {

    private String btName;
    private String btAddr;
    private String btState = "";
    private BluetoothDevice btDevice;

    public ListViewItem(String btName, String btAddr,BluetoothDevice btDevice){
        this.btName = btName;
        this.btAddr = btAddr;
        this.btDevice = btDevice;
    }

    public String getName() { return btName; }

    public String getAddr() {return btAddr; }

    public String getState() { return btState; }

    public BluetoothDevice getDevice() {
        return btDevice;
    }

    public void setState(String btState) {
        this.btState = btState;
    }

}
