package com.viatom.a20ftest;


import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.Disposable;


/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectFragment extends DialogFragment {

    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.success)
    TextView showSuccess;
    @BindView(R.id.spo2Val)
    TextView spo2Val;
    @BindView(R.id.prVal)
    TextView prVal;
    @BindView(R.id.logs)
    ListView logView;
    RxBleClient rxBleClient;
    RxBleDevice device;
    Disposable connection;
    Disposable connectionListener;
    Disposable notiListener;
    boolean passed = false;
    ArrayAdapter<String> adapter;
    private UUID notify = UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb");
    private UUID write = UUID.fromString("0000ffe9-0000-1000-8000-00805f9b34fb");
    private Context context;
    private Bluetooth b;

    private ArrayList<String> logs = new ArrayList<String>();
    private int spo2, pr = 0;

    public ConnectFragment() {
        // Required empty public constructor
    }

    @OnClick(R.id.disconnect)
    void disconnect() {
        this.dismiss();
        if (!connection.isDisposed()) {
            connection.dispose();
        }
        if (!connectionListener.isDisposed()) connectionListener.dispose();
    }

    public void setB(Bluetooth b) {
        this.b = b;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.fragment_connect, container, false);
        ButterKnife.bind(this, v);
        iniView();
        return v;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        connect(b);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showResult();
            }
        }, 15000);
    }

    void connect(Bluetooth b) {
        device = rxBleClient.getBleDevice(b.getMacAddr());
        connection = device.establishConnection(false) // <-- autoConnect flag
                .subscribe(
                        rxBleConnection -> {
                            notiListener = rxBleConnection.setupNotification(notify)
                                    .doOnNext(
                                            notificationObservable -> {
                                                addLogs("notify set up");
                                            }
                                    )
                                    .flatMap(notificationObservable -> notificationObservable)
                                    .subscribe(
                                            this::onNotifyReceived,
                                            throwable -> {
                                                addLogs(throwable.getMessage());
                                            }
                                    );

//                            byte[] bytesToWrite = new byte[5];
//                            bytesToWrite[0] = (byte) 0xfe;
//                            bytesToWrite[1] = (byte) 0x05;
//                            bytesToWrite[2] = (byte) 0x55;
//                            bytesToWrite[3] = (byte) 0x00;
//                            bytesToWrite[4] = CRCUtils.calCRC8(bytesToWrite);
//
//                            rxBleConnection.writeCharacteristic(write, bytesToWrite)
//                                    .subscribe(
//                                            bytes -> addLogs(HexString.bytesToHex(bytes)),
//                                            throwable -> addLogs(throwable.getMessage())
//                                    );
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

    }

    private void onNotifyReceived(byte[] bytes) {

        addLogs(HexString.bytesToHex(bytes));

        if (bytes.length == 18) {
            if ((bytes[0] & 0xff) == 0xfe && (bytes[1] & 0xff) == 0x08 && (bytes[2] & 0xff) == 0x56) {
                if (((bytes[11] & 0xff) == 0x01) && ((bytes[12] & 0xff) == 0xff)) {
                    pr = 0;
                } else {
                    pr = ((bytes[11] & 0xff) << 8) + (bytes[12] & 0xff);
                }
                if (bytes[13] == 0x7f) {
                    spo2 = 0;
                } else {
                    spo2 = bytes[13] & 0xff;
                }

                passed = true;
                showResult();
            } else if ((bytes[0] & 0xff) == 0xfe && (bytes[1] & 0xff) == 0x0a && (bytes[2] & 0xff) == 0x55) {
                if (((bytes[3] & 0xff) == 0x01) && ((bytes[4] & 0xff) == 0xff)) {
                    pr = 0;
                } else {
                    pr = ((bytes[3] & 0xff) << 8) + (bytes[4] & 0xff);
                }
                if (bytes[5] == 0x7f) {
                    spo2 = 0;
                } else {
                    spo2 = bytes[5] & 0xff;
                }

                passed = true;
                showResult();
            }
        } else if (bytes.length == 10) {
            if ((bytes[0] & 0xff) == 0xfe && (bytes[1] & 0xff) == 0x0a && (bytes[2] & 0xff) == 0x55) {
                if (((bytes[3] & 0xff) == 0x01) && ((bytes[4] & 0xff) == 0xff)) {
                    pr = 0;
                } else {
                    pr = ((bytes[3] & 0xff) << 8) + (bytes[4] & 0xff);
                }
                if (bytes[5] == 0x7f) {
                    spo2 = 0;
                } else {
                    spo2 = bytes[5] & 0xff;
                }

                passed = true;
                showResult();
            }
        }


    }

    void showResult() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    spo2Val.setText(String.valueOf(spo2));
                    prVal.setText(String.valueOf(pr));
                    if (passed) {
                        showSuccess.setText("测试通过 !");
                        showSuccess.setTextColor(Color.GREEN);
                        showSuccess.setVisibility(View.VISIBLE);
                    } else {
                        showSuccess.setText("测试不通过 !");
                        showSuccess.setTextColor(Color.RED);
                        showSuccess.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void addLogs(String s) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String time = format.format(System.currentTimeMillis());

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                logs.add(0, time + " " + s);
                adapter.notifyDataSetChanged();
            });
        }
    }

    private void iniView() {
        name.setText(b.getName());
        rxBleClient = RxBleClient.create(context);
        adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, android.R.id.text1, logs);
        logView.setAdapter(adapter);
    }
}
