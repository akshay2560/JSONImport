package com.example.jsonimport.BleApi;

import android.bluetooth.BluetoothDevice;

import no.nordicsemi.android.ble.BleManagerCallbacks;

public interface BleMeshManagerCallbacks extends BleManagerCallbacks {

    /**
     * Called when the node sends some data back to the provisioner
     *  @param bluetoothDevice
     * @param mtu
     * @param pdu the data received from the device
     */
    void onDataReceived(final BluetoothDevice bluetoothDevice, final int mtu, final byte[] pdu);

    /**
     * Called when the data has been sent to the connected device.
     *  @param device
     * @param mtu
     * @param pdu that was sent to the node
     */
    void onDataSent(final BluetoothDevice device, final int mtu, final byte[] pdu);
}
