package com.viatom.a20ftest.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.viatom.a20ftest.Bluetooth;
import com.viatom.a20ftest.HexString;
import com.viatom.a20ftest.R;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import io.reactivex.disposables.Disposable;

public class AutoConnectFragment extends Fragment {

    public static final String AUTO_CONNECT_DEVICE_NAME = "";
    private static final String ARG_PARAM1 = "param1";
    private final UUID CHARACTERISTIC_ID = UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb");
    RxBleClient rxBleClient;
    RxBleDevice device;
    Disposable connection;
    Disposable connectionListener;
    private Bluetooth bluetooth;

    @BindView(R.id.device_name)
    TextView deviceName;

    @BindView(R.id.value)
    TextView value;

    public AutoConnectFragment() {
        // Required empty public constructor
    }

    public static AutoConnectFragment newInstance(Bluetooth bluetooth) {
        AutoConnectFragment fragment = new AutoConnectFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, bluetooth);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bluetooth = (Bluetooth) getArguments().getSerializable(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_auto_connect, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        deviceName.setText(bluetooth.getName());
        connect(bluetooth);
    }

    @Override
    public void onDetach() {
        if (!connection.isDisposed()) connection.dispose();
        if (!connectionListener.isDisposed()) connectionListener.dispose();
        super.onDetach();
    }

    void connect(Bluetooth b) {
        device = rxBleClient.getBleDevice(b.getMacAddr());
        connection = device.establishConnection(false) // <-- autoConnect flag
                .subscribe(
                        rxBleConnection -> {
                            Disposable r = rxBleConnection
                                    .readCharacteristic(CHARACTERISTIC_ID)
                                    .repeatWhen(completed -> completed.delay(3, TimeUnit.SECONDS))
                                    .subscribe(
                                            bytes -> {
                                                addLogs(HexString.bytesToHex(bytes));
                                            },
                                            throwable -> {
                                                addLogs(throwable.getMessage());
                                            }
                                    );
                        },
                        throwable -> {
                            addLogs(throwable.getMessage());
                        }
                );

        connectionListener = device.observeConnectionStateChanges()
                .subscribe(
                        connectionState -> {
                            addLogs(connectionState.toString());
                        },
                        throwable -> {
                            addLogs(throwable.getMessage());
                        }
                );

//         When done... dispose and forget about connection teardown :)
//        disposable.dispose();
    }

    private void addLogs(String s) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String time = format.format(System.currentTimeMillis());

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                value.setText(String.format("%s %s", time, s));
            });
        }
    }
}