package com.oymotion.gforcedev;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.oymotion.gforcedev.utils.ToastUtil;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ${MouMou}
 * on 2017/5/24.
 */

public class DeviceScanHtmlActivity extends Activity {
    private WebView wv_device_scan;
    private static final int REQUEST_ENABLE_BT = 1;
    private int i = 0;
    private static final long SCAN_PERIOD = 1000;
    private ArrayList<BluetoothDevice> leDevices = new ArrayList<BluetoothDevice>();

    private BluetoothAdapter bluetoothAdapter;
    private Scanner scanner;
    private HashMap<BluetoothDevice, Integer> rssiMap = new HashMap<BluetoothDevice, Integer>();
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_device_scan);
        wv_device_scan = (WebView) findViewById(R.id.wv_device_scan);
        wv_device_scan.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        if (ContextCompat.checkSelfPermission(DeviceScanHtmlActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(DeviceScanHtmlActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        ActivityCompat.shouldShowRequestPermissionRationale(DeviceScanHtmlActivity.this,
                Manifest.permission.READ_CONTACTS);

        readHtmlFormAssets("DeviceScan.html");

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        wv_device_scan.loadUrl("javascript:funClear()");
        wv_device_scan.resumeTimers();
        if (leDevices!=null&&rssiMap!=null){
            leDevices.clear();
            rssiMap.clear();
            if (scanner!= null&&!scanner.isScanning){
                scanner.startScanning();
            }
            wv_device_scan.loadUrl("javascript:funUpdate()()");
        }
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!bluetoothAdapter.isEnabled()) {
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
        init();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
            } else {
                init();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (scanner != null) {
            scanner.stopScanning();
            scanner = null;
        }
    }

    private void init() {
        if (scanner == null) {
            scanner = new Scanner(bluetoothAdapter, mLeScanCallback);
            scanner.startScanning();
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (device!=null&&device.getName()!=null&&device.getName().contains("gForce")||device!=null&&device.getName()!=null&&device.getName().contains("OYM_")){
                                addDevice(device, rssi);
                                wv_device_scan.loadUrl("javascript:funUpdate()");
                            }
                        }
                    });
                }

            };

    private void readHtmlFormAssets(String url) {
        WebSettings webSettings = wv_device_scan.getSettings();
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setJavaScriptEnabled(true);
        wv_device_scan.addJavascriptInterface(new DeviceJavaScriptInterface(), "device");
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        wv_device_scan.setBackgroundColor(Color.TRANSPARENT);
        wv_device_scan.loadUrl("file:///android_asset/" + url);
    }


    private static class Scanner extends Thread {
        private final BluetoothAdapter bluetoothAdapter;
        private final BluetoothAdapter.LeScanCallback mLeScanCallback;

        private volatile boolean isScanning = false;

        Scanner(BluetoothAdapter adapter, BluetoothAdapter.LeScanCallback callback) {
            bluetoothAdapter = adapter;
            mLeScanCallback = callback;
        }

        public boolean isScanning() {
            return isScanning;
        }

        public void startScanning() {
            synchronized (this) {
                isScanning = true;
                start();
            }
        }

        public void stopScanning() {
            synchronized (this) {
                isScanning = false;
                bluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    synchronized (this) {
                        if (!isScanning)
                            break;

                        bluetoothAdapter.startLeScan(mLeScanCallback);
                    }

                    sleep(SCAN_PERIOD);

                    synchronized (this) {
                        bluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                }
            } catch (InterruptedException ignore) {
            } finally {
                bluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }

    final class DeviceJavaScriptInterface {
        DeviceJavaScriptInterface() {

        }

        @JavascriptInterface
        public int getDeviceSize() {
            return rssiMap.size();
        }

        @JavascriptInterface
        public String getDeviceName(int i) {
            if (leDevices.get(i).getName() == null) {
                return "UnKnownDevice";
            } else {
                return leDevices.get(i).getName();
            }
        }

        @JavascriptInterface
        public String getDeviceAddress(int i) {
            return leDevices.get(i).getAddress();
        }

        @JavascriptInterface
        public int getDeviceRssi(int i) {
            return rssiMap.get(leDevices.get(i));
        }

        @JavascriptInterface
        public void getClickItem(String i) {
            connectDevice(i);
        }

        @JavascriptInterface
        public void isScanStop(int i) {
            if (i == 1) {
                if (scanner == null) {
                    scanner = new Scanner(bluetoothAdapter, mLeScanCallback);
                    scanner.startScanning();
                }
            } else {
                if (scanner != null) {
                    scanner.stopScanning();
                    scanner = null;
                }
            }
        }
    }

    //get the click item,connect the clicked device
    private void connectDevice(String i) {
        int clickItem = Integer.valueOf(i);
        BluetoothDevice device = leDevices.get(clickItem);
        if (device == null)
            return;

        if (device.getName().contains("OAD")){
            final Intent intent = new Intent(DeviceScanHtmlActivity.this, DeviceOADHtmlActivity.class);
            intent.putExtra(DeviceOADHtmlActivity.EXTRAS_DEVICE_NAME, device.getName());
            intent.putExtra(DeviceOADHtmlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
            startActivity(intent);
        }else{
            final Intent intent = new Intent(DeviceScanHtmlActivity.this, DeviceServicesHtmlActivity.class);
            intent.putExtra(DeviceServicesHtmlActivity.EXTRAS_DEVICE_NAME, device.getName());
            intent.putExtra(DeviceServicesHtmlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
            startActivity(intent);
        }
    }

    //add the device which was scanned to the map
    public void addDevice(BluetoothDevice device, int rssi) {
        if (!leDevices.contains(device)) {
            leDevices.add(device);
        }
        rssiMap.put(device, rssi);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyWebView();

    }

    //Free webview memory
    public void destroyWebView() {
        if (wv_device_scan != null) {
            wv_device_scan.clearHistory();
            wv_device_scan.clearCache(true);
            wv_device_scan.loadUrl("about:blank"); // clearView() should be changed to loadUrl("about:blank"), since clearView() is deprecated now
            wv_device_scan.freeMemory();
            wv_device_scan = null; // Note that mWebView.destroy() and mWebView = null do the exact same thing
        }
    }
}

