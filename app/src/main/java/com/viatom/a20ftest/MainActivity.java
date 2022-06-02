package com.viatom.a20ftest;

import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.viatom.a20ftest.fragments.AutoConnectFragment;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private final static int CALL_PHONE_REQUEST_CODE = 521;
    private final static int REQUEST_TURN_ON_BT = 522;


    @BindView(R.id.bt_list)
    ListView bleList;
    @BindView(R.id.filter_rssi)
    SeekBar changeRssi;
    @BindView(R.id.filter_name)
    EditText changeName;
    @BindView(R.id.rssi_val)
    TextView rssiVal;
    BleAdapter adapter;
    @BindView(R.id.loading)
    private ProgressBar loading;

    private Context context;

    private String filterName = "";
    private int filterRssi = -100;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner leScanner;
    /**
     * lescan callback
     */
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            deviceFound(result);

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            //

        }

        @Override
        public void onScanFailed(int errorCode) {
            if (errorCode == SCAN_FAILED_ALREADY_STARTED) {
                showToast("already start");
            }
            if (errorCode == SCAN_FAILED_FEATURE_UNSUPPORTED) {
                showToast("scan settings not supported");
            }
            if (errorCode == 6) {
                showToast("too frequently");
            }
        }
    };

    public static boolean isLocationEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        return gps || network;
    }

    @OnClick(R.id.refresh)
    void refresh() {
        DeviceController.clear();
        setAdapter();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        ButterKnife.bind(this);

        iniAdapter();

        checkPermisson();
    }

    private void checkPermisson() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, CALL_PHONE_REQUEST_CODE);
        }

        // scan
        iniBT();
    }

    private void scanDevice(final boolean enable) {
        AsyncTask.execute(() -> {
            if (enable) {
                ScanSettings settings;
                if (Build.VERSION.SDK_INT >= 23) {
                    settings = new ScanSettings.Builder()
                            .setScanMode(SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(CALLBACK_TYPE_ALL_MATCHES)
                            .build();
                } else {
                    settings = new ScanSettings.Builder()
                            .setScanMode(SCAN_MODE_LOW_LATENCY)
                            .build();
                }

                leScanner.startScan(null, settings, leScanCallback);

            } else {
                if (leScanner != null && leScanCallback != null) {
                    if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                        leScanner.stopScan(leScanCallback);
                    }
                }
            }
        });
    }

    private void deviceFound(ScanResult result) {
        BluetoothDevice device = result.getDevice();
        if (device.getName() != null && device.getName().length() > 0) {
//            Log.d("20F", device.getAddress() + " <====== " + device.getName() + " ===> " + result.getRssi());

            Bluetooth b = new Bluetooth(device.getName(), device, result.getRssi());
            if (AutoConnectFragment.AUTO_CONNECT_DEVICE_NAME.equals(b.getName())) {
                addAutoConnectedDevice(b);
            }

            if (DeviceController.addDevice(b)) {
                /**
                 * need notify
                 */
                adapter.deviceList = DeviceController.getDevicesWithFilter(filterName, filterRssi);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void addAutoConnectedDevice(Bluetooth b) {
        loading.setVisibility(View.GONE);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.auto_connect, AutoConnectFragment.newInstance(b))
                .commit();
    }

    void setAdapter() {

        adapter = new BleAdapter(context, DeviceController.getDevicesWithFilter(filterName, filterRssi));
        adapter.setOnBleChooseListener(new BleAdapter.onBleChooseListener() {
            @Override
            public void onMyClick(Bluetooth b, View v) {
                connect(b);
            }
        });
        bleList.setAdapter(adapter);
    }

    void iniAdapter() {

        setAdapter();

        changeName.setText(filterName);
        changeName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                filterName = s.toString();
                adapter.deviceList = DeviceController.getDevicesWithFilter(filterName, filterRssi);
                adapter.notifyDataSetChanged();
            }
        });

        changeRssi.setProgress(60);
        changeRssi.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rssiVal.setText((-40 - progress) + " dbm");
                filterRssi = -40 - progress;
                adapter.deviceList = DeviceController.getDevicesWithFilter(filterName, filterRssi);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void connect(Bluetooth b) {
        showToast("try to connect " + b.getName());
        ConnectFragment fragment = new ConnectFragment();
        fragment.setB(b);
        fragment.setCancelable(false);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("connect");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        fragment.show(ft, "connect");
    }

    private void iniBT() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_TURN_ON_BT);
        } else if (!isLocationEnable(context) && Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 1);
        } else {
            BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = manager.getAdapter();
            leScanner = bluetoothAdapter.getBluetoothLeScanner();

            Log.d("20F", "start scan <===== ");
            scanDevice(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALL_PHONE_REQUEST_CODE) {
            checkPermisson();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        iniBT();
    }

    @Override
    public void onResume() {
        super.onResume();
        scanDevice(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onPause() {
        super.onPause();
        scanDevice(false);
        DeviceController.clear();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    void showToast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
}
