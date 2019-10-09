package com.example.jsonimport.ViewHolders;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.jsonimport.Models.ExtendedBluetoothDevice;
import com.example.jsonimport.R;

public class ScannedDevicesViewHolder extends RecyclerView.ViewHolder {

    public interface OnIdentifyClickListener {
        void onIdentifyClicked(ExtendedBluetoothDevice device);
    }

    private TextView deviceName,deviceMacId,identifyButton;
    private ExtendedBluetoothDevice device;

    public ScannedDevicesViewHolder(@NonNull View itemView,OnIdentifyClickListener onIdentifyClickListener) {
        super(itemView);
        deviceName = itemView.findViewById(R.id.deviceName);
        deviceMacId = itemView.findViewById(R.id.deviceMacId);
        identifyButton = itemView.findViewById(R.id.identify);
        identifyButton.setOnClickListener((view)->{
            onIdentifyClickListener.onIdentifyClicked(device);
        });
    }

    public void setItem(ExtendedBluetoothDevice device){
        this.device = device;
        deviceName.setText(device.getName());
        deviceMacId.setText(device.getAddress());
    }
}
