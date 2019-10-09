package com.example.jsonimport.Activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.jsonimport.BleApi.BleMeshManager;
import com.example.jsonimport.BleApi.ControlApi;
import com.example.jsonimport.Fragments.IdentifyDevice;
import com.example.jsonimport.Models.ExtendedBluetoothDevice;
import com.example.jsonimport.R;

import no.nordicsemi.android.meshprovisioner.MeshManagerApi;
import no.nordicsemi.android.meshprovisioner.provisionerstates.UnprovisionedMeshNode;

public class ProvisioningActivity extends AppCompatActivity implements IdentifyDevice.OnIdentifyDeviceActionListener{

    ProgressBar loading;
    TextView status;
    LinearLayout main2;
    BleMeshManager bleMeshManager;
    MeshManagerApi mMeshManagerApi;
    ControlApi controlApi;
    ExtendedBluetoothDevice extendedBluetoothDevice;
    Fragment activeFragment;
    UnprovisionedMeshNode unprovisionedNode;
    String macID;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("deviceDisconnected")) {
                finish();
            } else if (intent.getAction().equals("provIncomplete")) {
                setResultIntent();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provisioning);

        main2 = findViewById(R.id.main2);
        loading = findViewById(R.id.loading);
        status = findViewById(R.id.status);

        bleMeshManager = BleMeshManager.getInstance(this);
        mMeshManagerApi = MeshManagerApi.getInstance(this);
        controlApi = ControlApi.getInstance(this);

        macID = getIntent().getStringExtra("macID");
        controlApi.setMacId(macID);

        final Intent intent = getIntent();
        extendedBluetoothDevice = intent.getParcelableExtra(ControlApi.EXTRA_DEVICE);
        controlApi.connect(extendedBluetoothDevice);

        controlApi.isDeviceReady.observe(this, aVoid -> {
            if(controlApi.isDeviceReady()){
                main2.setVisibility(View.GONE);
                status.setVisibility(View.GONE);
                loading.setVisibility(View.GONE);
                getSupportFragmentManager().beginTransaction().replace(R.id.main, activeFragment = new IdentifyDevice()).commitAllowingStateLoss();
                if(!controlApi.isProvisionComplete()) {
                    controlApi.identifyNode(extendedBluetoothDevice);
                }
                Log.e("Prov Activity", controlApi.isProvisionComplete()+"");
                if(controlApi.isProvisionComplete()){
                    Log.e("Prov Activity", "Provision Complete");
                    getSupportFragmentManager().beginTransaction().remove(activeFragment).commitAllowingStateLoss();
                    main2.setVisibility(View.VISIBLE);
                    status.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.VISIBLE);
                }
            }
        });

        controlApi.unprovisionedMeshNodeMutableLiveData.observe(this, unprovisionedMeshNode -> {
            if (unprovisionedMeshNode != null) {
                unprovisionedNode = unprovisionedMeshNode;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("DeviceReady");
        intentFilter.addAction("unprovisionedNode");
        intentFilter.addAction("provIncomplete");
        intentFilter.addAction("deviceDisconnected");
        intentFilter.addAction("identify");
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onIdentifyDeviceAction(boolean proceed) {
        if (!proceed) {
            controlApi.disconnect();
            finish();
            return;
        }
        getSupportFragmentManager().beginTransaction().remove(activeFragment).commitAllowingStateLoss();
        main2.setVisibility(View.VISIBLE);
        status.setVisibility(View.VISIBLE);
        loading.setVisibility(View.VISIBLE);
        if (unprovisionedNode != null) {
            controlApi.initiateProvisioning(unprovisionedNode);
        }
    }



    private void setResultIntent() {
        if (bleMeshManager.isProvisioningComplete()) {
            Log.e("Provisioning", "Complete");
            final Intent returnIntent = new Intent();
            returnIntent.putExtra(ControlApi.PROVISIONING_COMPLETED, true);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        } else {
            final Intent returnIntent = new Intent();
            returnIntent.putExtra(ControlApi.PROVISIONING_COMPLETED, false);
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        }

    }
}
