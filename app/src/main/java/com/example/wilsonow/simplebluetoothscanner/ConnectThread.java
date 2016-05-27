package com.example.wilsonow.simplebluetoothscanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class ConnectThread extends Thread {
    private final BluetoothSocket btSocket;
    private final BluetoothDevice btDevice;
    private final BluetoothAdapter btAdapter;
    private Context context;

    public ConnectThread(BluetoothDevice btDevice, BluetoothSocket btSocket, BluetoothAdapter btAdapter, Context context) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        this.btSocket = btSocket;
        this.btDevice = btDevice;
        this.btAdapter = btAdapter;
        this.context = context;
    }

    public void run() {
        // Cancel discovery because it will slow down the connection
        btAdapter.cancelDiscovery();

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            btSocket.connect();
            Log.i("ConnectThread", "Connected11!");
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
                btSocket.close();
                Log.i("ConnectThread555", connectException.toString());
            } catch (IOException closeException) { }
            return;
        }

        // Do work to manage the connection (in a separate thread)
        //manageConnectedSocket(mmSocket);
        Toast.makeText(context, "Connected!", Toast.LENGTH_SHORT).show();
        Log.i("ConnectThread", "Connected!");
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            btSocket.close();
        } catch (IOException e) {
            Log.i("ConnectThread", e.toString());
        }
    }
}