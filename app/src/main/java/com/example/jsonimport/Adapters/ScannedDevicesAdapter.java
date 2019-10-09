package com.example.jsonimport.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.jsonimport.Models.ExtendedBluetoothDevice;
import com.example.jsonimport.R;
import com.example.jsonimport.ViewHolders.ScannedDevicesViewHolder;

import java.util.List;

public class ScannedDevicesAdapter extends RecyclerView.Adapter<ScannedDevicesViewHolder> {

    private final String TAG = ScannedDevicesAdapter.class.getSimpleName();
    private Context context;
    private List<ExtendedBluetoothDevice> devices;
    private ScannedDevicesViewHolder.OnIdentifyClickListener onIdentifyClickListener;

    public ScannedDevicesAdapter(Context context, List<ExtendedBluetoothDevice> devices, ScannedDevicesViewHolder.OnIdentifyClickListener onIdentifyClickListener){
        this.context = context;
        this.devices = devices;
        this.onIdentifyClickListener = onIdentifyClickListener;
    }

    @NonNull
    @Override
    public ScannedDevicesViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_device_list, viewGroup, false);
        return new ScannedDevicesViewHolder(itemView,onIdentifyClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ScannedDevicesViewHolder scannedDevicesViewHolder, int i) {
        ExtendedBluetoothDevice device = devices.get(i);
        scannedDevicesViewHolder.setItem(device);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }
}
