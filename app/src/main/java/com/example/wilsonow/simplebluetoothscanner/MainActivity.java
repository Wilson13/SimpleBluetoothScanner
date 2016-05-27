package com.example.wilsonow.simplebluetoothscanner;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ArrayList<ListViewItem> availableArrayList;
    private ArrayList<ListViewItem> pairedArrayList;
    private BluetoothAdapter btAdapter;
    private BluetoothAdapter bleBtAdapter;
    private BluetoothManager bluetoothManager;
    private BluetoothSocket btSocket;
    private ListView lvAvailableDevices;
    private ListView lvPairedDevices;
    private ListViewAdapter lvAvailableAdapter;
    private ListViewAdapter lvPairedAdapter;
    private SwipeRefreshLayout swipeContainer;

    private int currentItemPos = 0;
    public static int REQUEST_BLUETOOTH = 1;
    final int SCAN_PERIOD = 10000; // scan for 10 seconds
    //private UUID bitwaveUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private UUID bitwaveUUID = UUID.fromString("00001107-D102-11E1-9B23-00025B00A5A5");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize
        init();
        getBondedDevice();
    }

    private void init() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    }
                });
                builder.show();
            }
        }

        // Set up variables
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        // Set up conventional Bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Set up BLE
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleBtAdapter = bluetoothManager.getAdapter();

        // Check if Bluetooth and BLE are supported
        checkBtStatus();

        // Set up custom ArrayList and ListViewAdapter, and ListView for available devices
        lvAvailableDevices = (ListView) findViewById(R.id.lv_available_devices);
        availableArrayList = new ArrayList<>();
        lvAvailableAdapter = new ListViewAdapter(MainActivity.this, availableArrayList);
        lvAvailableDevices.setAdapter(lvAvailableAdapter);
        lvAvailableDevices.setEmptyView(findViewById(R.id.empty_list_item));

        // Set up custom ArrayList and ListViewAdapter, and ListView for paired devices
        lvPairedDevices = (ListView) findViewById(R.id.lv_paired_devices);
        pairedArrayList = new ArrayList<>();
        lvPairedAdapter = new ListViewAdapter(MainActivity.this, pairedArrayList);
        lvPairedDevices.setAdapter(lvPairedAdapter);

        lvAvailableDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Log.i("Pairing", "Clicked");
                /** Need to care for multiple clicks on items **/
                currentItemPos = position;
                availableArrayList.get(position).setState("Connecting...");
                lvAvailableAdapter.notifyDataSetChanged();
                BluetoothDevice deviceToConnect = availableArrayList.get(position).getDevice();
                pairDevice(deviceToConnect);
                availableArrayList.get(position).setState("Connected");
            }
        });

        lvPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Log.i("Pairing", "Clicked");
                /** Need to care for multiple clicks on items **/
                currentItemPos = position;
                pairedArrayList.get(position).setState("Connecting...");
                lvPairedAdapter.notifyDataSetChanged();
                BluetoothDevice deviceToConnect = pairedArrayList.get(position).getDevice();
                pairDevice(deviceToConnect);
                pairedArrayList.get(position).setState("Connected");
            }
        });

        // Set up refresh listener which triggers discovery of devices
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getBondedDevice();
                scanForDevices();
                scanForBLEDevice();
            }
        });

        // Set up scan-for-devices button
        findViewById(R.id.empty_list_item).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // It wont trigger the circle to animate, so by adding a delay in the UI thread it shows the circle animation inside the UI thread.
                swipeContainer.post(new Runnable() {
                    @Override
                    public void run() {
                        swipeContainer.setRefreshing(true); // This is just to show the refreshing animation
                        scanForDevices();
                        scanForBLEDevice();
                    }
                });
            }
        });
    }

    // BroadcastReceiver for receiving scanned Bluetooth devices
    private BroadcastReceiver btDiscoveryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Log.d("DeviceList", "Bluetooth device found\n" + action);
            /*if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) { */

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("DeviceList", "Bluetooth device found\n" + device.getName());

                // If device doesn't exist in the list and is Basic Rate/Enhanced Data Rate (BR/EDR) device (conventional BT)
                if ( !checkDuplicatedBtDevice(device.getAddress()) && device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC ) {
                    findViewById(R.id.empty_list_item).setVisibility(View.GONE);
                    ListViewItem newDevice = new ListViewItem(device.getName(), device.getAddress(), device); // Create a new device item
                    availableArrayList.add(newDevice); // Add it to our adapter
                    lvAvailableAdapter.notifyDataSetChanged();

                    // Remove duplicates
                    /*ArrayList<ListViewItem> newList = new ArrayList<>(new LinkedHashSet<>(availableArrayList));
                    availableArrayList.clear();
                    availableArrayList.addAll(newList);
                    lvAvailableAdapter.notifyDataSetChanged();
                    newList.clear();*/
                }

            } else if ( BluetoothDevice.ACTION_ACL_CONNECTED.equals(action) ) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i("ACTION_ACL_CONNECTED", "Addr: "+ device.getAddress() + " Name: " + device.getName());
                stateConnectedPairedDevices(device.getAddress());
            }
        }
    };

    private BroadcastReceiver btPairingReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Log.i("PAIRING", BluetoothDevice.EXTRA_BOND_STATE);

            if ( BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action) ) {

                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Toast.makeText(MainActivity.this, "Paired", Toast.LENGTH_SHORT).show();
                    availableArrayList.get(currentItemPos).setState("Connecting");
                    lvAvailableAdapter.notifyDataSetChanged();
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(MainActivity.this, "Unpaired", Toast.LENGTH_SHORT).show();
                    availableArrayList.get(currentItemPos).setState(" ");
                    lvAvailableAdapter.notifyDataSetChanged();
                }
            } else if ( BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action) ) {
                stopRefreshing();
            }
        }
    };

    private void checkBtStatus() {
        // Phone does not support Bluetooth so let the user know and exit.
        if (btAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        else if (!btAdapter.isEnabled()) { // If BT is not turned on
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_BLUETOOTH);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
        }

        if ( bleBtAdapter == null ) {
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth Low Energy")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        else if (!bleBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH);
        }

    }

    private Boolean checkDuplicatedBtDevice(String btAddr) {
        if ( availableArrayList.size() > 0 ) {
            for (int i = 0; i < availableArrayList.size(); i++) {
                if ( availableArrayList.get(i).getAddr().equals(btAddr) ) {
                    //Log.i("Duplicated", availableArrayList.get(i).getName());
                    return true;
                }
            }
        }
        if ( pairedArrayList.size() > 0 ) {
            for (int i = 0; i < pairedArrayList.size(); i++) {
                if ( pairedArrayList.get(i).getAddr().equals(btAddr) ) {
                    Log.i("Duplicated in paired", pairedArrayList.get(i).getName());
                    return true;
                }
            }
        }
        return false;
    }

    private void getBondedDevice() {
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        pairedArrayList.clear(); // Reset
        lvPairedAdapter.notifyDataSetChanged();

        // If there are paired devices
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) { // Loop through paired devices
                //Log.i("Paired", device.getName());
                ListViewItem newDevice = new ListViewItem(device.getName(), device.getAddress(), device); // Create a new device item
                pairedArrayList.add(newDevice);
                lvPairedAdapter.notifyDataSetChanged();
            }
        }
    }

    private void scanForDevices() {

        lvAvailableDevices.setEnabled(false); // Disable click on listview items
        // Set up Bluetooth scanning result receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(btDiscoveryReceiver, filter);

        availableArrayList.clear();
        btAdapter.startDiscovery();
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            public void run() {
                btAdapter.cancelDiscovery();
                stopRefreshing();
            }
        };
        handler.postDelayed(runnable, SCAN_PERIOD);
    }

    private void scanForBLEDevice() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bleBtAdapter.startLeScan(lowLvlBleScanCallback);
            }
        }, 0);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bleBtAdapter.stopLeScan(lowLvlBleScanCallback);
            }
        }, SCAN_PERIOD);
    }

    private BluetoothAdapter.LeScanCallback lowLvlBleScanCallback =
        new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi,
                                 byte[] scanRecord) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("onLeScan", device.getName());
                        //connectToDevice(device);
                    }
                });
            }
        };

    private void pairDevice(BluetoothDevice device) {

        /**
         * Gaia Control actually creates the socket, so we only need to create the bond.
         * When creating an RFComm socket, pairing is prompted and required by the Android phone.
         */
        try {
            btSocket = device.createRfcommSocketToServiceRecord(bitwaveUUID);
            Log.i("btSocket", " btSocket created without error");
            //btSocket = device.createRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());
        } catch (Exception e) {
            Log.e("btSocket", "Rfcomm Error: "+e);
        }

        ConnectThread connThread = new ConnectThread(device, btSocket, btAdapter, MainActivity.this);
        connThread.run();

        /**
         * Try to see if this enable the phone to create two rfcomm connections.
         */
        if (connThread.isAlive() ) {

            ConnectThread connSecondThread = new ConnectThread(device, btSocket, btAdapter, MainActivity.this);
            connSecondThread.run();

        }

        /*try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                device.createBond();
            } else {
                Method method = device.getClass().getMethod("createBond", (Class[]) null);
                method.invoke(device, (Object[]) null);
            }
        } catch (Exception e) {
            Log.e("Pair device", "Error: "+e);
        }*/
    }

    private void stopRefreshing() {
        try {
            unregisterReceiver(btDiscoveryReceiver);
        } catch (Exception e) {
            Log.e("MainActivity", "unregisterReceiver error");
        }
        swipeContainer.setRefreshing(false);
        lvAvailableDevices.setEnabled(true);
    }

    private void stateConnectedPairedDevices(String btAddr) {

        for (int i = 0; i < pairedArrayList.size(); i++) {
            if(pairedArrayList.get(i).getAddr().equals(btAddr)) {
                pairedArrayList.get(i).setState("Connected");
                lvPairedAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(btPairingReceiver, filter);
    }

    @Override
    public void onPause(){
        super.onPause();

        try {
            //unregisterReceiver(btDiscoveryReceiver);
            unregisterReceiver(btPairingReceiver);
            btSocket.close();
        } catch (Exception e) {
            Log.e("MainActivity", "unregisterReceiver error");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
