package com.example.jsonimport.Activities;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jsonimport.Adapters.ScannedDevicesAdapter;
import com.example.jsonimport.BleApi.ControlApi;
import com.example.jsonimport.Models.ExtendedBluetoothDevice;
import com.example.jsonimport.R;
import com.example.jsonimport.Room.MyRoomDatabase;
import com.example.jsonimport.Shared.Config;
import com.example.jsonimport.ViewHolders.ScannedDevicesViewHolder;

import java.util.ArrayList;

import no.nordicsemi.android.meshprovisioner.MeshManagerApi;

public class Scanner extends AppCompatActivity implements ScannedDevicesViewHolder.OnIdentifyClickListener {

    private RecyclerView discoveredDeviceRecyclerView;
    private ArrayList<ExtendedBluetoothDevice> discoveredDevicesList;
    private ScannedDevicesAdapter scannedDevicesAdapter;
    private ControlApi controlApi;
    private String macId;
    private MyRoomDatabase myRoomDatabase;
    private MeshManagerApi meshManagerApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        myRoomDatabase = MyRoomDatabase.getDatabase(this);
        controlApi = ControlApi.getInstance(this);
        meshManagerApi = MeshManagerApi.getInstance(this);

        discoveredDeviceRecyclerView = findViewById(R.id.discoveredDevices);

        discoveredDevicesList = new ArrayList<>();
        discoveredDevicesList.clear();

        scannedDevicesAdapter = new ScannedDevicesAdapter(this,discoveredDevicesList,this);
        discoveredDeviceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        discoveredDeviceRecyclerView.setItemAnimator(new DefaultItemAnimator());
        discoveredDeviceRecyclerView.setAdapter(scannedDevicesAdapter);

        if (getIntent().getStringExtra("macID") == null) {
            Toast.makeText(getApplicationContext(), "Please Choose a File", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }else {
            macId = getIntent().getStringExtra("macId");
        }

        controlApi.bluetoothDeviceMutableLiveData.observe(this, extendedBluetoothDevice -> {
            if(extendedBluetoothDevice!=null) {
                if (!listContains(extendedBluetoothDevice)) {
                    discoveredDevicesList.add(extendedBluetoothDevice);
                    scannedDevicesAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!ControlApi.isLocationPermissionsGranted(this)) {
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                    .setTitle(getApplicationContext().getString(R.string.app_name))
                    .setMessage("Location permission is required for BLE. Please turn it on")
                    .setPositiveButton("Ok", null)
                    .setOnDismissListener(dialogInterface -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.FOREGROUND_SERVICE}, Config.PERMISSION_REQUEST);
                        } else {
                            Intent locationPermissionIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            locationPermissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            locationPermissionIntent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(locationPermissionIntent);
                        }
                    }).show();
            ((TextView) dialog.findViewById(android.R.id.message)).setTypeface(Config.typefaceMedium);
        } else if (!ControlApi.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        } else if (!ControlApi.isLocationEnabled(this)) {
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                    .setMessage("Location (Network) is required to be turned on for Bluetooth LE")
                    .setPositiveButton("Turn On", (dialogInterface, i) -> {
                        Intent locationEnableIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(locationEnableIntent);
                    }).show();
            ((TextView) dialog.findViewById(android.R.id.message)).setTypeface(Config.typefaceMedium);
        } else {
            controlApi.startScan(ControlApi.MESH_PROVISIONING_UUID);
        }
    }

    @Override
    public void onIdentifyClicked(ExtendedBluetoothDevice device) {
        if (myRoomDatabase.myDao().getUnicastAddress(macId) == null) {
            Toast.makeText(getApplicationContext(), "Please save the file", Toast.LENGTH_SHORT).show();
            return;
        }
        meshManagerApi.getMeshNetwork().setUnicastAddress(Integer.parseInt(myRoomDatabase.myDao().getUnicastAddress(macId),16));
        controlApi.nullifyProvisionedNode();
        Intent provisioningActivity = new Intent(this, ProvisioningActivity.class);
        provisioningActivity.putExtra("EXTRA_DEVICE", device);
        provisioningActivity.putExtra("macID", this.macId);
        startActivityForResult(provisioningActivity, ControlApi.PROVISIONING_SUCCESS);
    }

    private boolean listContains(ExtendedBluetoothDevice device) {
        for (ExtendedBluetoothDevice extendedBluetoothDevice : discoveredDevicesList) {
            if (extendedBluetoothDevice.matches(device.getScanResult())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ControlApi.PROVISIONING_SUCCESS) {
            if (resultCode == RESULT_OK)
                finish();
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Provisioning Incomplete\nPlease Retry", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        controlApi.stopScan();
    }

    @Override
    protected void onStop() {
        super.onStop();
        controlApi.stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        controlApi.stopScan();
    }
}
