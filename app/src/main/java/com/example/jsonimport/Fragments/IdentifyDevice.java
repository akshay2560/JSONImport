package com.example.jsonimport.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.jsonimport.R;
import com.example.jsonimport.Shared.Config;

public class IdentifyDevice extends Fragment {

    private TextView identifyTextBold, identifyTextNormal, setupDevice, cancelText, setupText, startText, observeDeviceMessage;
    private ImageView deviceType;
    private LinearLayout cancelButton, setupButton, startButton;
    private OnIdentifyDeviceActionListener listener;
    private Handler handler = new Handler();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_identify_device, container, false);


        identifyTextBold = view.findViewById(R.id.identifyTextBold);
        identifyTextNormal = view.findViewById(R.id.identifyTextNormal);
        deviceType = view.findViewById(R.id.deviceType);
        setupDevice = view.findViewById(R.id.setupDevice);
        cancelText = view.findViewById(R.id.cancelText);
        setupText = view.findViewById(R.id.setupText);
        observeDeviceMessage = view.findViewById(R.id.observeDeviceMessage);
        cancelButton = view.findViewById(R.id.cancelButton);
        setupButton = view.findViewById(R.id.setupButton);
        startText = view.findViewById(R.id.startText);
        startButton = view.findViewById(R.id.startButton);

        identifyTextBold.setTypeface(Config.typefaceBold);
        identifyTextNormal.setTypeface(Config.typefaceRegular);
        setupDevice.setTypeface(Config.typefaceMedium);
        cancelText.setTypeface(Config.typefaceBold);
        setupText.setTypeface(Config.typefaceBold);
        startText.setTypeface(Config.typefaceBold);
        observeDeviceMessage.setTypeface(Config.typefaceMedium);


        cancelButton.setVisibility(View.VISIBLE);
        setupButton.setVisibility(View.GONE);
        startButton.setVisibility(View.GONE);

        handler.postDelayed(() -> startButton.setVisibility(View.VISIBLE),5000);

        cancelButton.setOnClickListener(v -> {
            if(listener!=null)
                listener.onIdentifyDeviceAction(false);
        });

        startButton.setOnClickListener(v -> {
            if(listener!=null)
                listener.onIdentifyDeviceAction(true);
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof OnIdentifyDeviceActionListener) {
            listener = (OnIdentifyDeviceActionListener) context;
        }
        else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }


    public interface OnIdentifyDeviceActionListener {
        void onIdentifyDeviceAction(boolean proceed);
    }
}
