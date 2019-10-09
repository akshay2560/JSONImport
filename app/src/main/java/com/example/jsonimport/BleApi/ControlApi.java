package com.example.jsonimport.BleApi;

import android.Manifest;
import android.arch.lifecycle.MutableLiveData;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.jsonimport.Models.ExtendedBluetoothDevice;
import com.example.jsonimport.Room.MyRoomDatabase;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.meshprovisioner.Group;
import no.nordicsemi.android.meshprovisioner.MeshManagerApi;
import no.nordicsemi.android.meshprovisioner.MeshManagerCallbacks;
import no.nordicsemi.android.meshprovisioner.MeshNetwork;
import no.nordicsemi.android.meshprovisioner.MeshProvisioningStatusCallbacks;
import no.nordicsemi.android.meshprovisioner.MeshStatusCallbacks;
import no.nordicsemi.android.meshprovisioner.Provisioner;
import no.nordicsemi.android.meshprovisioner.UnprovisionedBeacon;
import no.nordicsemi.android.meshprovisioner.provisionerstates.ProvisioningState;
import no.nordicsemi.android.meshprovisioner.provisionerstates.UnprovisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.transport.ConfigAppKeyAdd;
import no.nordicsemi.android.meshprovisioner.transport.ConfigAppKeyStatus;
import no.nordicsemi.android.meshprovisioner.transport.ConfigCompositionDataGet;
import no.nordicsemi.android.meshprovisioner.transport.ConfigCompositionDataStatus;
import no.nordicsemi.android.meshprovisioner.transport.ConfigModelAppBind;
import no.nordicsemi.android.meshprovisioner.transport.ConfigModelAppStatus;
import no.nordicsemi.android.meshprovisioner.transport.ConfigModelPublicationGet;
import no.nordicsemi.android.meshprovisioner.transport.ConfigModelPublicationSet;
import no.nordicsemi.android.meshprovisioner.transport.ConfigModelPublicationStatus;
import no.nordicsemi.android.meshprovisioner.transport.ConfigModelSubscriptionAdd;
import no.nordicsemi.android.meshprovisioner.transport.ConfigModelSubscriptionStatus;
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage;
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageAcked;
import no.nordicsemi.android.meshprovisioner.transport.VendorModelMessageStatus;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanRecord;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class ControlApi implements MeshStatusCallbacks, MeshManagerCallbacks, BleMeshManagerCallbacks, MeshProvisioningStatusCallbacks {
    private Handler handler = new Handler();
    private static ControlApi controlApi;
    private Context context;
    private MeshManagerApi meshManagerApi;
    private BleMeshManager bleMeshManager;
    private MeshNetwork mMeshNetwork;

    private ExtendedBluetoothDevice extendedBluetoothDevice;
    private UUID mFilterUuid;
    public final static UUID MESH_PROXY_UUID = UUID.fromString("00001828-0000-1000-8000-00805F9B34FB");
    public final static UUID MESH_PROVISIONING_UUID = UUID.fromString("00001827-0000-1000-8000-00805F9B34FB");

    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    public static final String PROVISIONING_COMPLETED = "PROVISIONING_COMPLETED";

    public static final int PROVISIONING_SUCCESS = 2112;
    private final static int ADVERTISED_NETWORK_ID_OFFSET = 1;
    private static final int ADVERTISEMENT_TYPE_NETWORK_ID = 0x00;
    private final static int ADVERTISED_HASH_LENGTH = 8;
    private final static int ADVERTISED_NETWORK_ID_LENGTH = 8;

    private ProvisionedMeshNode provisionedMeshNode;
    private UnprovisionedMeshNode unprovisionedMeshNode;

    public MutableLiveData<ProvisionedMeshNode> provisionedMeshNodeMutableLiveData = new MutableLiveData<>();

    public MutableLiveData<UnprovisionedMeshNode> unprovisionedMeshNodeMutableLiveData = new MutableLiveData<>();

    public MutableLiveData<ExtendedBluetoothDevice> bluetoothDeviceMutableLiveData = new MutableLiveData<>();

    public MutableLiveData<Boolean> connectionStatus = new MutableLiveData<>();

    public final MutableLiveData<Void> isDeviceReady = new MutableLiveData<>();

    private static final int ATTENTION_TIMER = 5;
    private boolean mSetupProvisionedNode ,isProvisionComplete = false,isReconnecting,afterProv,cancelOnIdentify = false;

    private String TAG = ControlApi.class.getSimpleName();

    public static MyRoomDatabase myRoomDatabase;
    private int accessControlUnitGroupAddress;
    private byte[] barrIdBarrDir;
    private byte[] encryptionKey;
    private int gatewayGroupAddress;
    private int healthServerGroupAddress;
    private int unicastAddress;

    public static ControlApi getInstance(Context context) {
        if (controlApi == null) {
            controlApi = new ControlApi(context);
        }
        return controlApi;
    }

    private ControlApi(Context context) {
        this.context = context;
        meshManagerApi = MeshManagerApi.getInstance(context);
        bleMeshManager = BleMeshManager.getInstance(context);
        meshManagerApi.loadMeshNetwork();
        meshManagerApi.setMeshManagerCallbacks(this);
        meshManagerApi.setMeshStatusCallbacks(this);
        meshManagerApi.setProvisioningStatusCallbacks(this);
        bleMeshManager.setGattCallbacks(this);
        myRoomDatabase = MyRoomDatabase.getDatabase(context);
    }

    public void setMacId(String macId){
        unicastAddress = Integer.parseInt(myRoomDatabase.myDao().getUnicastAddress(macId),16);
        accessControlUnitGroupAddress = Integer.parseInt(myRoomDatabase.myDao().getAccessControlUnitGroupAddress(macId), 16);
        encryptionKey = hexToByteArray("70"+myRoomDatabase.myDao().getEncryptionKey(macId));
        barrIdBarrDir = hexToByteArray("71"+myRoomDatabase.myDao().getBarrierId(macId)+myRoomDatabase.myDao().getBarrierDirection(macId));
        healthServerGroupAddress = Integer.parseInt(myRoomDatabase.myDao().getHealthServerGroupAddress(macId),16);
        gatewayGroupAddress = Integer.parseInt(myRoomDatabase.myDao().getGatewayGroupAddress(macId),16);
    }


    private void loadNetwork(final MeshNetwork meshNetwork) {
        mMeshNetwork = meshNetwork;
        if (mMeshNetwork != null) {
            if (!mMeshNetwork.isProvisionerSelected()) {
                final Provisioner provisioner = meshNetwork.getProvisioners().get(0);
                provisioner.setLastSelected(true);
                mMeshNetwork.selectProvisioner(provisioner);
            }
        }
    }



    @Override
    public void onDataReceived(BluetoothDevice bluetoothDevice, int mtu, byte[] pdu) {
        meshManagerApi.handleNotifications(mtu, pdu);
    }

    @Override
    public void onDataSent(BluetoothDevice device, int mtu, byte[] pdu) {
        meshManagerApi.handleWriteCallbacks(mtu, pdu);
    }

    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {
        connectionStatus.postValue(false);
        Log.e(TAG, "Device Connecting");
    }

    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {
        connectionStatus.postValue(false);
        Log.e(TAG, "Device Connected");
    }

    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
        connectionStatus.postValue(false);
        Log.e(TAG, "Device Disconnecting");
    }

    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device) {
        Log.e(TAG, "Device Disconnected");
        connectionStatus.postValue(false);
        if(isReconnecting) {
            isReconnecting = false;
            Intent intent = new Intent();
            intent.setAction("provIncomplete");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }else if(afterProv){
            Log.e("Disconnected After","Provisioning");
        }else if(cancelOnIdentify){
            Log.e("Disconnected after","Identifying");
            Intent intent = new Intent();
            intent.setAction("identify");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } else{
            Intent intent = new Intent();
            intent.setAction("deviceDisconnected");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
        mSetupProvisionedNode = false;
    }

    @Override
    public void onLinkLossOccurred(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onServicesDiscovered(@NonNull BluetoothDevice device, boolean optionalServicesFound) {

    }

    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {
        Log.e(TAG, "Device Ready");
        isDeviceReady.postValue(null);
        if (bleMeshManager.isProvisioningComplete()) {
            Log.e(TAG, "onDeviceReady: Provisioning Complete" );
            if (mSetupProvisionedNode) {
                Log.e(TAG, "onDeviceReady: Provisioned Node Setup" );
                connectionStatus.postValue(true);
                final ProvisionedMeshNode node = provisionedMeshNode;
                final ConfigCompositionDataGet configCompositionDataGet = new ConfigCompositionDataGet();
                handler.postDelayed(() -> meshManagerApi.sendMeshMessage(node.getUnicastAddress(), configCompositionDataGet),2000);
            }
        }
    }

    @Override
    public void onBondingRequired(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onBonded(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onBondingFailed(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode) {
        Log.e(TAG, "Error: " + message + " Error Code: " + errorCode + " Device: " + device.getAddress());
    }

    @Override
    public void onDeviceNotSupported(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onNetworkLoaded(MeshNetwork meshNetwork) {
        loadNetwork(meshNetwork);
    }

    @Override
    public void onNetworkUpdated(MeshNetwork meshNetwork) {
        loadNetwork(meshNetwork);
    }

    @Override
    public void onNetworkLoadFailed(String error) {

    }

    @Override
    public void onNetworkImported(MeshNetwork meshNetwork) {

    }

    @Override
    public void onNetworkImportFailed(String error) {

    }

    @Override
    public void onNetworkExported(MeshNetwork meshNetwork) {

    }

    @Override
    public void onNetworkExportedJson(MeshNetwork meshNetwork, String networkJson) {

    }

    @Override
    public void onNetworkExportFailed(String error) {

    }

    @Override
    public void sendProvisioningPdu(UnprovisionedMeshNode meshNode, byte[] pdu) {
        bleMeshManager.sendPdu(pdu);
    }

    @Override
    public void sendMeshPdu(byte[] pdu) {
        bleMeshManager.sendPdu(pdu);
    }

    @Override
    public int getMtu() {
        return bleMeshManager.getMtuSize();
    }

    @Override
    public void onTransactionFailed(byte[] dst, boolean hasIncompleteTimerExpired) {

    }

    @Override
    public void onTransactionFailed(int dst, boolean hasIncompleteTimerExpired) {

    }

    @Override
    public void onUnknownPduReceived(byte[] src, byte[] accessPayload) {

    }

    @Override
    public void onUnknownPduReceived(int src, byte[] accessPayload) {

    }

    @Override
    public void onBlockAcknowledgementSent(byte[] dst) {

    }

    @Override
    public void onBlockAcknowledgementSent(int dst) {

    }

    @Override
    public void onBlockAcknowledgementReceived(byte[] src) {

    }

    @Override
    public void onBlockAcknowledgementReceived(int src) {

    }

    @Override
    public void onMeshMessageSent(byte[] dst, MeshMessage meshMessage) {

    }

    @Override
    public void onMeshMessageSent(int dst, MeshMessage meshMessage) {

    }

    @Override
    public void onMeshMessageReceived(byte[] src, MeshMessage meshMessage) {

    }

    @Override
    public void onMeshMessageReceived(int src, MeshMessage meshMessage) {
        Log.e("On Mesh", "Received");
        if(meshMessage instanceof ConfigCompositionDataStatus){
            Log.e("On Mesh", "ConfigCompositionData Status");
            ConfigAppKeyAdd configAppKeyAdd = new ConfigAppKeyAdd(meshManagerApi.getMeshNetwork().getPrimaryNetworkKey(),meshManagerApi.getMeshNetwork().getAppKey(0));
            meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(),configAppKeyAdd);
        }else if(meshMessage instanceof ConfigAppKeyStatus){
            Log.e("On Mesh", "ConfigAppKey Status");
            if (provisionedMeshNode.getUuid().substring(0, 4).equals("1001")) {
                bindKeyForUnitHealth();
            } else if (provisionedMeshNode.getUuid().substring(0, 4).equals("100b")) {
                bindKeyForGatewayHealthServer();
            }
        }else if(meshMessage instanceof ConfigModelAppStatus){
            Log.e("On Mesh", "ConfigModelApp Status");
            ConfigModelAppStatus status = (ConfigModelAppStatus) meshMessage;
            if(status.getModelIdentifier() == 0x0002){
                Log.e("On Mesh", "ConfigModelApp 0002");
                if (provisionedMeshNode.getUuid().substring(0, 4).equals("1001")) {
                    Log.e("On Mesh", "ConfigModelApp 0059000A");
                    bindKeyForUnitVendorModelGateway();
                } else if (provisionedMeshNode.getUuid().substring(0, 4).equals("100b")) {
                    bindKeyForGatewayHealthClient();
                }
            }else if(status.getModelIdentifier() == 0x0003){
                bindKeyForGatewayVendorModelGateway();
            }else if(status.getModelIdentifier() == 0x0059000A){
                if (provisionedMeshNode.getUuid().substring(0, 4).equals("1001")) {
                    bindKeyForUnitVendorModelUnit();
                } else if (provisionedMeshNode.getUuid().substring(0, 4).equals("100b")) {
                    bindKeyForGatewayVendorModelUnit();
                }
            }else if(status.getModelIdentifier() == 0x0059000B){
                Log.e("On Mesh", "ConfigModelApp 0059000B");
                if (provisionedMeshNode.getUuid().substring(0, 4).equals("1001")) {
                    publishForUnitHealthServer();
                } else if (provisionedMeshNode.getUuid().substring(0, 4).equals("100b")) {
                    publishForGatewayHealthModel();
                }
            }
        }else if(meshMessage instanceof ConfigModelPublicationStatus){
            Log.e("On Mesh", "ConfigModelPublication Status");
            ConfigModelPublicationStatus status = (ConfigModelPublicationStatus) meshMessage;
            if(status.getModelIdentifier() == 0x0002){
                Log.e("On Mesh", "ConfigModelPublication model 0002");
                if (provisionedMeshNode.getUuid().substring(0, 4).equals("1001")) {
                    publishForUnitGatewayModel();
                } else if (provisionedMeshNode.getUuid().substring(0, 4).equals("100b")) {
                    subscribeForHealthClient();
                }
            }else if(status.getModelIdentifier() == 0x0059000A){
                Log.e("On Mesh", "ConfigModelPublication model 0059000A");
                if (provisionedMeshNode.getUuid().substring(0, 4).equals("1001")) {
                    subscribeForUnitModel();
                } else if (provisionedMeshNode.getUuid().substring(0, 4).equals("100b")) {
                    subscribeForGatewayModel();
                }
            }
        }else if(meshMessage instanceof ConfigModelSubscriptionStatus){
            Log.e("On Mesh", "ConfigModelSubscriptionStatus model 0059000B");
            ConfigModelSubscriptionStatus status = (ConfigModelSubscriptionStatus) meshMessage;
            if (status.getModelIdentifier() == 3) {
                publishForUnitModel();
            }else{
                if (provisionedMeshNode.getUuid().substring(0, 4).equals("1001")) {
                    Log.e("On Mesh", "Unit model 0059000B");
                    sendEncryptionKey();
                } else if (provisionedMeshNode.getUuid().substring(0, 4).equals("100b")) {
                    Log.e("On Mesh", "Gateway model 0059000B");
                    provisionedMeshNodeMutableLiveData.postValue(provisionedMeshNode);
                    Intent intent = new Intent();
                    intent.setAction("DeviceKey");
                    intent.putExtra("deviceKey", MeshParserUtils.bytesToHex(provisionedMeshNode.getDeviceKey(), false));
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
            }
        }else if(meshMessage instanceof VendorModelMessageStatus){
            Log.e("On Mesh", "VendorModelMessage Status");
            Log.e("Data Received", MeshParserUtils.bytesToHex(((VendorModelMessageStatus) meshMessage).getAccessPayload(), false));
            if (((VendorModelMessageStatus) meshMessage).getAccessPayload()[3] == 70) {
                sendBarrierIdAndBarrierDirection();
                Log.e("Vendor Model", "70");
            }else if(((VendorModelMessageStatus) meshMessage).getAccessPayload()[3] == 71){
                Log.e("Vendor Model", "71");
                sendDF();
            }else if(((VendorModelMessageStatus) meshMessage).getAccessPayload()[3]==0xDF){
                Log.e("Vendor Model", "DF");
                provisionedMeshNodeMutableLiveData.postValue(provisionedMeshNode);
                Intent intent = new Intent();
                intent.setAction("DeviceKey");
                intent.putExtra("deviceKey", MeshParserUtils.bytesToHex(provisionedMeshNode.getDeviceKey(), false));
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        }
    }

    @Override
    public void onMessageDecryptionFailed(String meshLayer, String errorMessage) {

    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(mFilterUuid.equals(MESH_PROVISIONING_UUID)){
                byte[] serviceData = result.getScanRecord().getServiceData(new ParcelUuid(ControlApi.MESH_PROVISIONING_UUID));
                if(serviceData!=null) {
                    if (serviceData[1] == 0x01 || serviceData[1] == 0x0b) {
                        Log.e(TAG, "onScanResult: " + MeshParserUtils.bytesToHex(serviceData, false));
                        extendedBluetoothDevice = new ExtendedBluetoothDevice(result);
                        bluetoothDeviceMutableLiveData.postValue(extendedBluetoothDevice);
                    }
                }
            }else if(mFilterUuid.equals(MESH_PROXY_UUID)) {
                final ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord != null) {
                    final byte[] serviceData = getServiceData(result, ControlApi.MESH_PROXY_UUID);
                    if (serviceData != null) {
                        if (meshManagerApi.isAdvertisedWithNodeIdentity(serviceData)) {
                            final ProvisionedMeshNode meshNode = provisionedMeshNode;
                            if (meshManagerApi.nodeIdentityMatches(meshNode, serviceData)) {
                                stopScan();
                                onProvisionedDeviceFound(meshNode, new ExtendedBluetoothDevice(result));
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    @Override
    public void onProvisioningStateChanged(UnprovisionedMeshNode meshNode, ProvisioningState.States state, byte[] data) {
        unprovisionedMeshNode = meshNode;
        unprovisionedMeshNodeMutableLiveData.postValue(meshNode);
        switch (state) {
            case PROVISIONING_INVITE:
                break;
            case PROVISIONING_FAILED:
                isProvisionComplete = false;
                break;

        }
    }

    @Override
    public void onProvisioningFailed(UnprovisionedMeshNode meshNode, ProvisioningState.States state, byte[] data) {
        unprovisionedMeshNodeMutableLiveData.postValue(meshNode);
        unprovisionedMeshNode = meshNode;
        switch(state) {
            case PROVISIONING_FAILED:
                isProvisionComplete = false;
                break;
        }
    }

    @Override
    public void onProvisioningCompleted(ProvisionedMeshNode meshNode, ProvisioningState.States state, byte[] data) {
        provisionedMeshNode = meshNode;
        unprovisionedMeshNodeMutableLiveData.postValue(null);
        switch(state){
            case PROVISIONING_COMPLETE:
                onProvisioningComplete(meshNode);
                break;
        }

    }

    private void onProvisioningComplete(final ProvisionedMeshNode node) {
        Log.e(TAG, "onProvisioningComplete");
        isProvisionComplete = true;
        provisionedMeshNode = node;
        afterProv = true;
        bleMeshManager.disconnectDevice();
        bleMeshManager.refreshDeviceCacheMemory();
        handler.postDelayed(() -> startScan(MESH_PROXY_UUID),1000);
    }


    private void onProvisionedDeviceFound(final ProvisionedMeshNode node, final ExtendedBluetoothDevice device) {
        mSetupProvisionedNode = true;
        provisionedMeshNode = node;
        isReconnecting = true;
        //Added an extra delay to ensure reconnection
        handler.postDelayed(() -> connectToProxy(device),2000);
    }

    public  void initiateProvisioning(UnprovisionedMeshNode unprovisionedMeshNode) {
        if (unprovisionedMeshNode.getProvisioningCapabilities() != null) {
            meshManagerApi.startProvisioning(unprovisionedMeshNode);
        }
    }

    private void connectToProxy(final ExtendedBluetoothDevice device) {
        bleMeshManager.connectToDevice(device.getDevice());
    }

    public void startScan(UUID filterUUID) {
        // Scanning settings
        mFilterUuid = filterUUID;
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                // Refresh the devices list every second
                .setReportDelay(0)
                // Hardware filtering has some issues on selected devices
                .setUseHardwareFilteringIfSupported(false)
                // Samsung S6 and S6 Edge report equal value of RSSI for all devices. In this app we ignore the RSSI.
                /*.setUseHardwareBatchingIfSupported(false)*/
                .build();

        // Let's use the filter to scan only for Mesh devices
        final List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid((ControlApi.MESH_PROXY_UUID))).build());
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid((ControlApi.MESH_PROVISIONING_UUID))).build());
        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.startScan(filters, settings, scanCallback);
        Log.v("Provisioning", "Scan started");
        handler.postDelayed(this::stopScan, 20000);
    }

    public void stopScan() {
        Log.e(TAG,"Stop Scan Yes");
        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.stopScan(scanCallback);
    }

    public void identifyNode(final ExtendedBluetoothDevice device) {
        final UnprovisionedBeacon beacon = (UnprovisionedBeacon) device.getBeacon();
        if (beacon != null) {
            meshManagerApi.identifyNode(beacon.getUuid(), device.getName(), ATTENTION_TIMER);
        } else {
            final byte[] serviceData = getServiceData(device.getScanResult(), BleMeshManager.MESH_PROVISIONING_UUID);
            Log.e("Service Data", MeshParserUtils.bytesToHex(serviceData,false));
            if (serviceData != null) {
                final UUID uuid = meshManagerApi.getDeviceUuid(serviceData);
                Log.e("Service Data", uuid.toString());
                meshManagerApi.identifyNode(uuid, device.getName(), ATTENTION_TIMER);
                cancelOnIdentify = true;
            }
        }
    }

    public void connect(ExtendedBluetoothDevice extendedBluetoothDevice){
        isProvisionComplete = false;
        BluetoothDevice device = extendedBluetoothDevice.getDevice();
        handler.postDelayed(() -> bleMeshManager.connectToDevice(device),1000);

    }

    public void nullifyProvisionedNode(){
        clearProvisioningLiveData();
    }

    public void disconnect(){
        isProvisionComplete = false;
        removeCallbacks();
        clearProvisioningLiveData();
        bleMeshManager.disconnectDevice();
    }

    private void clearProvisioningLiveData(){
        unprovisionedMeshNodeMutableLiveData.setValue(null);
        provisionedMeshNodeMutableLiveData.setValue(null);
    }


    private void removeCallbacks() {
        handler.removeCallbacksAndMessages(null);
    }

    public boolean isDeviceReady(){
        return bleMeshManager.isDeviceReady();
    }

    public boolean isProvisionComplete(){
        return isProvisionComplete;
    }

    @Nullable
    public static byte[] getServiceData(@NonNull final ScanResult result, @NonNull final UUID serviceUuid) {
        final ScanRecord scanRecord = result.getScanRecord();
        if (scanRecord != null) {
            return scanRecord.getServiceData(new ParcelUuid((serviceUuid)));
        }
        return null;
    }

    public static boolean isLocationPermissionsGranted(final Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isBluetoothEnabled() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
    }

    public static boolean isLocationEnabled(final Context context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm != null //Check if location is on
                    && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);//True if either of battery saver mode or High Accuracy mode is selected
        } else
            return true;

    }

    public static byte[] hexToByteArray(String hex) {
        hex = hex.length() % 2 != 0 ? "0" + hex : hex;

        byte[] b = new byte[hex.length() / 2];

        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(hex.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }

    private void bindKeyForUnitHealth() {
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), new ConfigModelAppBind(unicastAddress, 0x0002, 0));
    }

    private void bindKeyForUnitVendorModelGateway(){
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), new ConfigModelAppBind(unicastAddress, 0x0059000A, 0));
    }

    private void bindKeyForUnitVendorModelUnit(){
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), new ConfigModelAppBind(unicastAddress, 0x0059000B, 0));
    }

    private void bindKeyForGatewayHealthServer(){
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), new ConfigModelAppBind(unicastAddress, 0x0002, 0));
    }

    private void bindKeyForGatewayHealthClient(){
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), new ConfigModelAppBind(unicastAddress, 0x0003, 0));
    }

    private void bindKeyForGatewayVendorModelGateway(){
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), new ConfigModelAppBind(unicastAddress, 0x0059000A, 0));
    }

    private void bindKeyForGatewayVendorModelUnit(){
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), new ConfigModelAppBind(unicastAddress, 0x0059000B, 0));
    }

    private void publishForUnitHealthServer() {
        ConfigModelPublicationSet configModelPublicationset = new ConfigModelPublicationSet(unicastAddress, healthServerGroupAddress,
                0,
                false,
                10,
                3,
                2,
                0,
                0,
                0x0002);
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), configModelPublicationset);
    }

    private void publishForUnitGatewayModel() {
        ConfigModelPublicationSet configModelPublicationset = new ConfigModelPublicationSet(unicastAddress, gatewayGroupAddress,
                0,
                false,
                10,
                0,
                0,
                0,
                0,
                0x0059000A);
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), configModelPublicationset);
    }

    private void publishForUnitModel() {
        ConfigModelPublicationSet configModelPublicationset = new ConfigModelPublicationSet(unicastAddress, accessControlUnitGroupAddress,
                0,
                false,
                10,
                0,
                0,
                0,
                0,
                0x0059000A);
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), configModelPublicationset);
    }

    private void publishForGatewayHealthModel() {
        ConfigModelPublicationSet configModelPublicationset = new ConfigModelPublicationSet(unicastAddress, healthServerGroupAddress,
                0,
                false,
                10,
                3,
                2,
                0,
                0,
                0x0002);
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), configModelPublicationset);
    }

    private void subscribeForUnitModel() {
        Group group = new Group(accessControlUnitGroupAddress, meshManagerApi.getMeshNetwork().getMeshUUID());
        group.setName("UNO");
        meshManagerApi.getMeshNetwork().addGroup(group);
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), new ConfigModelSubscriptionAdd(unicastAddress, accessControlUnitGroupAddress, 0x0059000B));
    }

    private void subscribeForHealthClient() {
        Group group = new Group(healthServerGroupAddress, meshManagerApi.getMeshNetwork().getMeshUUID());
        group.setName("Health");
        meshManagerApi.getMeshNetwork().addGroup(group);
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), new ConfigModelSubscriptionAdd(unicastAddress, accessControlUnitGroupAddress, 0x0003));
    }

    private void subscribeForGatewayModel() {
        Group group = new Group(gatewayGroupAddress, meshManagerApi.getMeshNetwork().getMeshUUID());
        group.setName("Gateway");
        meshManagerApi.getMeshNetwork().addGroup(group);
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), new ConfigModelSubscriptionAdd(unicastAddress, accessControlUnitGroupAddress, 0x0059000B));
    }

    private void sendEncryptionKey() {
        VendorModelMessageAcked vendorModelMessageAcked = new VendorModelMessageAcked(meshManagerApi.getMeshNetwork().getAppKey(0).getKey(), 0x0059000B, 89, 0xC5, encryptionKey);
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), vendorModelMessageAcked);
    }

    private void sendBarrierIdAndBarrierDirection() {
        VendorModelMessageAcked vendorModelMessageAcked = new VendorModelMessageAcked(meshManagerApi.getMeshNetwork().getAppKey(0).getKey(), 0x0059000B, 89, 0xC5, barrIdBarrDir);
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), vendorModelMessageAcked);
    }

    private void sendDF() {
        VendorModelMessageAcked vendorModelMessageAcked = new VendorModelMessageAcked(meshManagerApi.getMeshNetwork().getAppKey(0).getKey(), 0x0059000B, 89, 0xC5, hexToByteArray("DF01"));
        meshManagerApi.sendMeshMessage(provisionedMeshNode.getUnicastAddress(), vendorModelMessageAcked);
    }


}
