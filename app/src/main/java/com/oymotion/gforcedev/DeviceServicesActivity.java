/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 111111111111111
 */

package com.oymotion.gforcedev;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.oymotion.gforcedev.adapters.BleServicesAdapter;
import com.oymotion.gforcedev.adapters.BleServicesAdapter.OnServiceItemClickListener;
import com.oymotion.gforcedev.gforce_control.GForceData;
import com.oymotion.gforcedev.gforce_control.Quaternion;
import com.oymotion.gforcedev.gforce_service.gForceDataService;
import com.oymotion.gforcedev.gforce_service.gForceOadResetService;
import com.oymotion.gforcedev.gforce_service.gForceOadService;
import com.oymotion.gforcedev.gforce_service.gForceService;
import com.oymotion.gforcedev.gforce_service.gForceServices;
import com.oymotion.gforcedev.info_service.BleDeviceInfoService;
import com.oymotion.gforcedev.info_service.BleGapService;
import com.oymotion.gforcedev.info_service.BleInfoService;
import com.oymotion.gforcedev.info_service.BleInfoServices;
import com.oymotion.gforcedev.ui.view.LoadingDialog;
import com.oymotion.gforcedev.utils.ContentUriUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static java.lang.Boolean.TRUE;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BleService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceServicesActivity extends Activity {
    private final static String TAG = DeviceServicesActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private static final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 1;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 1;

    private boolean isWebShow = false;

    private TextView tv_service_title;
    private TextView connectionState;
    private TextView dataText;
    private TextView dataField;
    private TextView gestureText;
    private TextView gestureField;
    private ProgressBar oadProgress;
    private TextView oadProgressText;
    private int oadProgressVal = 0;

    private WebView wv_guesture;
    private WebSettings m_WebSettings;

    private ExpandableListView gattServicesList;
    private BleServicesAdapter gattServiceAdapter;

    private String deviceName;
    private String deviceAddress;
    private BleService bleService;
    private boolean isConnected = false;

    private byte[] imageData = new byte[0];
    private BluetoothGattService oadSrv = null;
    private BluetoothGattCharacteristic oadImgIdentifyChar = null;
    private BluetoothGattCharacteristic oadImgBlockChar = null;
    private int blockNum = 0;

    // gForce Data related params.
    private static long mPackageCount = -1;
    private static int mLastPackageID = -1;
    private static long mTotallyLostPackageCount = 0;
    private static long mPreviousTime;

    private LoadingDialog dialog;

    private OnServiceItemClickListener serviceListener;

    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BleService.LocalBinder) service).getService();
            if (!bleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            bleService.connect(deviceAddress);
            dialog.setContent("Connecting...");
            dialog.show();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i("12138",action.toString());
            if (BleService.ACTION_GATT_CONNECTED.equals(action)) {
                isConnected = true;
                dialog.dismiss();
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i(TAG, "Disconnected from GATT server in DeviceServiceActivity");
                isConnected = false;
                changeTextCorlor(2);
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(bleService.getSupportedGattServices());

                if (gattServiceAdapter != null) {
                    // If GAP service discovered, read characteristic value.
                    if (gattServiceAdapter.containsGroup(BleGapService.UUID_SERVICE)) {
                        if (gattServiceAdapter.containsChild(BleGapService.UUID_DEVICE_NAME)) {
                            bleService.read(BleGapService.UUID_SERVICE, BleGapService.UUID_DEVICE_NAME, null);
                        }
                        if (gattServiceAdapter.containsChild(BleGapService.UUID_APPEARANCE)) {
                            bleService.read(BleGapService.UUID_SERVICE, BleGapService.UUID_APPEARANCE, null);
                        }
                        if (gattServiceAdapter.containsChild(BleGapService.UUID_PPF)) {
                            bleService.read(BleGapService.UUID_SERVICE, BleGapService.UUID_PPF, null);
                        }
                        if (gattServiceAdapter.containsChild(BleGapService.UUID_RECCONECTION_ADDRESS)) {
                            bleService.read(BleGapService.UUID_SERVICE, BleGapService.UUID_RECCONECTION_ADDRESS, null);
                        }
                        if (gattServiceAdapter.containsChild(BleGapService.UUID_PPCP)) {
                            bleService.read(BleGapService.UUID_SERVICE, BleGapService.UUID_PPCP, null);
                        }
                    }

                    // If device information service discovered, read characteristic value.
                    if (gattServiceAdapter.containsGroup(BleDeviceInfoService.UUID_SERVICE)) {
                        if (gattServiceAdapter.containsChild(BleDeviceInfoService.UUID_SYSTEM_ID)) {
                            bleService.read(BleDeviceInfoService.UUID_SERVICE, BleDeviceInfoService.UUID_SYSTEM_ID, null);
                        }
                        if (gattServiceAdapter.containsChild(BleDeviceInfoService.UUID_MODEL_NUMBER)) {
                            bleService.read(BleDeviceInfoService.UUID_SERVICE, BleDeviceInfoService.UUID_MODEL_NUMBER, null);
                        }
                        if (gattServiceAdapter.containsChild(BleDeviceInfoService.UUID_SERIAL_NUMBER)) {
                            bleService.read(BleDeviceInfoService.UUID_SERVICE, BleDeviceInfoService.UUID_SERIAL_NUMBER, null);
                        }
                        if (gattServiceAdapter.containsChild(BleDeviceInfoService.UUID_FIRMWARE_REV)) {
                            bleService.read(BleDeviceInfoService.UUID_SERVICE, BleDeviceInfoService.UUID_FIRMWARE_REV, null);
                        }
                        if (gattServiceAdapter.containsChild(BleDeviceInfoService.UUID_HARDWARE_REV)) {
                            bleService.read(BleDeviceInfoService.UUID_SERVICE, BleDeviceInfoService.UUID_HARDWARE_REV, null);
                        }
                        if (gattServiceAdapter.containsChild(BleDeviceInfoService.UUID_SOFTWARE_REV)) {
                            bleService.read(BleDeviceInfoService.UUID_SERVICE, BleDeviceInfoService.UUID_SOFTWARE_REV, null);
                        }
                        if (gattServiceAdapter.containsChild(BleDeviceInfoService.UUID_MANUFACTURER_NAME)) {
                            bleService.read(BleDeviceInfoService.UUID_SERVICE, BleDeviceInfoService.UUID_MANUFACTURER_NAME, null);
                        }
                        if (gattServiceAdapter.containsChild(BleDeviceInfoService.UUID_CERT)) {
                            bleService.read(BleDeviceInfoService.UUID_SERVICE, BleDeviceInfoService.UUID_CERT, null);
                        }
                        if (gattServiceAdapter.containsChild(BleDeviceInfoService.UUID_PNP_ID)) {
                            bleService.read(BleDeviceInfoService.UUID_SERVICE, BleDeviceInfoService.UUID_PNP_ID, null);
                        }
                    }

                    // If gForce Data Service discovered
                    if (gattServiceAdapter.containsGroup(gForceDataService.UUID_SERVICE)) {
                        dataField.setVisibility(View.VISIBLE);
                        dataText.setVisibility(View.VISIBLE);
                        gestureText.setVisibility(View.VISIBLE);
                        gestureField.setVisibility(View.VISIBLE);
                    }

                    // If gForce OAD service discovered
                    if (gattServiceAdapter.containsGroup(gForceOadService.UUID_SERVICE)) {
                        // first set oad progress bar visible.
                        oadProgress.setVisibility(View.VISIBLE);
                        oadProgressText.setVisibility(View.VISIBLE);

                        // enable characteristic notification, to prepare for OAD flow.
                        if (gattServiceAdapter.containsChild(gForceOadService.UUID_IMG_IDENTIFY)) {
                            bleService.notifyConfig(gForceOadService.UUID_SERVICE, gForceOadService.UUID_IMG_IDENTIFY, TRUE);
                            Log.d(TAG, "Enable OAD image identity char notify");
                        }
                        if (gattServiceAdapter.containsChild(gForceOadService.UUID_IMG_BLOCK)) {
                            bleService.notifyConfig(gForceOadService.UUID_SERVICE, gForceOadService.UUID_IMG_BLOCK, TRUE);
                            Log.d(TAG, "Enable OAD image block char notify");
                        }
                    }
                }
            } else if (BleService.ACTION_DATA_AVAILABLE.equals(action)) {
                String srvUuid = intent.getStringExtra(BleService.EXTRA_SERVICE_UUID);
                String charUuid = intent.getStringExtra(BleService.EXTRA_CHARACTERISTIC_UUID);
                byte[] data = intent.getByteArrayExtra(BleService.EXTRA_DATA);

                // GAP profile data
                if (srvUuid.equals(BleGapService.UUID_SERVICE)) {
                    if (charUuid.equals(BleGapService.UUID_DEVICE_NAME)) {
                        BleInfoServices.getService(BleGapService.UUID_SERVICE).setCharacteristicValue(
                                BleGapService.UUID_DEVICE_NAME, new String(data));
                    } else if (charUuid.equals(BleGapService.UUID_APPEARANCE)) {
                        BleInfoServices.getService(BleGapService.UUID_SERVICE).setCharacteristicValue(
                                BleGapService.UUID_APPEARANCE, string2HexString(data));
                    } else if (charUuid.equals(BleGapService.UUID_PPF)) {

                    } else if (charUuid.equals(BleGapService.UUID_RECCONECTION_ADDRESS)) {

                    } else if (charUuid.equals(BleGapService.UUID_PPCP)) {
                        double connIntervalMin = ((data[0] & 0xFF) | ((data[1] & 0xFF) << 8)) * 1.25;
                        double connIntervalMax = ((data[2] & 0xFF) | ((data[3] & 0xFF) << 8)) * 1.25;
                        int connLatency = ((data[4] & 0xFF) | ((data[5] & 0xFF) << 8));
                        int connTimeout = ((data[6] & 0xFF) | ((data[7] & 0xFF) << 8)) * 10;

                        BleInfoServices.getService(BleGapService.UUID_SERVICE).setCharacteristicValue(
                                BleGapService.UUID_PPCP, connIntervalMin + " " + connIntervalMax + " "
                                        + connLatency + " " + connTimeout);
                    }
                }
                // Device information data
                else if (srvUuid.equals(BleDeviceInfoService.UUID_SERVICE)) {
                    if (charUuid.equals(BleDeviceInfoService.UUID_SYSTEM_ID)
                            || charUuid.equals(BleDeviceInfoService.UUID_CERT)
                            || charUuid.equals(BleDeviceInfoService.UUID_PNP_ID)) {
                        BleInfoServices.getService(srvUuid).setCharacteristicValue(
                                charUuid, string2HexString(data));
                    } else if (charUuid.equals(BleDeviceInfoService.UUID_MODEL_NUMBER)
                            || charUuid.equals(BleDeviceInfoService.UUID_SERIAL_NUMBER)
                            || charUuid.equals(BleDeviceInfoService.UUID_FIRMWARE_REV)
                            || charUuid.equals(BleDeviceInfoService.UUID_HARDWARE_REV)
                            || charUuid.equals(BleDeviceInfoService.UUID_SOFTWARE_REV)
                            || charUuid.equals(BleDeviceInfoService.UUID_MANUFACTURER_NAME)) {
                        BleInfoServices.getService(srvUuid).setCharacteristicValue(
                                charUuid, new String(data));
                    }
                }
                // OAD profile data
                else if (srvUuid.equals(gForceOadService.UUID_SERVICE)) {
                    if (charUuid.equals(gForceOadService.UUID_IMG_BLOCK)) {
                        Log.d(TAG, "OAD image block number received");
                        Log.d(TAG, "data len: " + data.length);

                        int reqBlockNum = (int) ((data[0] & 0xFF) + ((data[1] & 0xFF) << 8));
                        Log.d(TAG, "reqBlockNum = " + reqBlockNum);
						
						// Fix OAD difference process in gForce OAD R2 and R3
                        if ( (reqBlockNum == 0) || (reqBlockNum == 1) ) {
                            blockNum = reqBlockNum;
                        }
                        else {
                            // do nothing
                        }
						
                        if ((reqBlockNum == blockNum)
                                && ((blockNum * 16) < imageData.length)
                                && (data.length == 2)) {
                            byte[] blockBuf = readBlock(blockNum, imageData);
                            byte[] blockWithBlkNumBuf = addBlknumToBlock(data, blockBuf);

                            Log.d(TAG, "imageData Len " + imageData.length);
                            Log.d(TAG, "blockNum: " + blockNum + ",  blockBuf len: " + blockWithBlkNumBuf.length);
                            dumpBytes(blockWithBlkNumBuf);
                            // Write OAD Image Block Characteristic.
                            bleService.write(gForceOadService.UUID_SERVICE,
                                    gForceOadService.UUID_IMG_BLOCK,
                                    blockWithBlkNumBuf);

                            blockNum++;

                            int tempProgress = (int) (((blockNum + 1) * 1600) / imageData.length);

                            if (oadProgressVal != tempProgress) {
                                oadProgressVal = tempProgress;
                                String progressStr = oadProgressVal + "%";
                                oadProgress.setProgress(oadProgressVal);
                                oadProgressText.setText(progressStr);
                            }
                        }
						else {
                            Log.d(TAG, "reqBlockNum != blockNum");
                        }
						
                        if (data.length != 2) {
                            dumpBytes(data);
                        }

                    } else if (charUuid.equals(gForceOadService.UUID_IMG_IDENTIFY)) {
                        Log.d(TAG, "OAD image identify char notify recieved");
                    }
                }
                // gForce Data profile
                else if (srvUuid.equals(gForceDataService.UUID_SERVICE)) {
                    Log.i("12138","start");
                    if (charUuid.equals(gForceDataService.UUID_GFORCE_DATA)) {
                        displayData(data);
                    }
                }
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  For example
    // 'Read', 'Write', 'Notify' and 'Indicate' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long i1d) {
                    Log.d(TAG, "item click");
                    if (gattServiceAdapter == null)
                        return false;

                    final BluetoothGattService service = gattServiceAdapter.getGroup(groupPosition);
                    final BluetoothGattCharacteristic characteristic = gattServiceAdapter.getChild(groupPosition, childPosition);

                    final BleInfoService infoService = BleInfoServices
                            .getService(service.getUuid().toString());
                    final gForceService gforceService = gForceServices
                            .getService(service.getUuid().toString());

                    if (infoService != null) {
                        Log.d(TAG, "infoService != null");
                        if (characteristic.getProperties() == BluetoothGattCharacteristic.PROPERTY_READ) {
                            bleService.read(service.getUuid().toString(),
                                    characteristic.getUuid().toString(), null);
                            Log.d(TAG, "read char");
                        }
                    } else if (gforceService != null) {
                        Log.d(TAG, "gforceService != null");
                        if (gforceService.getUUID().equals(gForceOadResetService.UUID_SERVICE)) {
                            // gForce firmware reset Service
                            if (characteristic.getUuid().toString().equals(gForceOadResetService.UUID_RESET)) {
                                // reset characteristic
                                byte[] wrtVal = {0x01, 0x02, 0x03};
                                bleService.write(service.getUuid().toString(),
                                        characteristic.getUuid().toString(),
                                        wrtVal);
                                Log.d(TAG, "OAD reset write");
                            }
                        } else if (gforceService.getUUID().equals(gForceDataService.UUID_SERVICE)) {
                            // gForce Data Service
                            Log.d(TAG, "gForce Data Service");
                            if (characteristic.getUuid().toString().equals(gForceDataService.UUID_GFORCE_DATA)) {
                                // gForce data characteristic
                                bleService.notifyConfig(service.getUuid().toString(),
                                        characteristic.getUuid().toString(),
                                        TRUE);
                                Log.d(TAG, "gForce data notify enable");
                            }
                        } else if (gforceService.getUUID().equals(gForceOadService.UUID_SERVICE)) {
                            // gForce OAD Service
                            Log.d(TAG, "gForce OAD Service");
							

                            oadSrv = service;
                            oadImgIdentifyChar = characteristic;

                            // select a binary file to write
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            // any file format
                            intent.setType("*/*");
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            startActivityForResult(intent, 1);
                        }
                    }

                    return true;
                }
            };

    private final OnServiceItemClickListener demoClickListener = new OnServiceItemClickListener() {
        @Override
        public void onDemoClick(BluetoothGattService service) {
            Log.d(TAG, "onDemoClick: service" + service.getUuid().toString());
        }

        @Override
        public void onServiceEnabled(BluetoothGattService service, boolean enabled) {
            // may enable some notifications here.
        }

        @Override
        public void onServiceUpdated(BluetoothGattService service) {

        }
    };
    private Quaternion q;
    private TextView tv_connection_state;
    private TextView tv_device_address;
    private TextView device_address;

    private String string2HexString(byte[] data) {
        final StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte byteChar : data)
            stringBuilder.append(String.format("%02X ", byteChar));
        return stringBuilder.toString();
    }

    private void clearUI() {
        gattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        dataField.setText(R.string.no_data);
        gestureField.setText(R.string.no_data);
    }

    public void setServiceListener(OnServiceItemClickListener listener) {
        this.serviceListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        dialog = new LoadingDialog(DeviceServicesActivity.this);

        tv_service_title = (TextView) findViewById(R.id.tv_service_title);
        wv_guesture = (WebView) findViewById(R.id.wv_guesture);

        m_WebSettings = wv_guesture.getSettings();
        m_WebSettings.setJavaScriptEnabled(true);
        wv_guesture.addJavascriptInterface(new GuestureJavaScriptInterface(), "guesture");
        m_WebSettings.setAllowUniversalAccessFromFileURLs(true);

        wv_guesture.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        String url = "file:///android_asset/webglm.html";
//        String url = "file:///android_asset/webgl.html";

        wv_guesture.loadUrl(url);

        // Sets up UI references.
        gattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        gattServicesList.setOnChildClickListener(servicesListClickListner);

        device_address = (TextView) findViewById(R.id.device_address);
        connectionState = (TextView) findViewById(R.id.connection_state);
        tv_connection_state = (TextView) findViewById(R.id.tv_connection_state);
        tv_device_address = (TextView) findViewById(R.id.tv_device_address);
        gestureText = (TextView) findViewById(R.id.gesture_text);
        gestureField = (TextView) findViewById(R.id.gesture_value);
        oadProgress = (ProgressBar) findViewById(R.id.oad_progress);
        oadProgressText = (TextView) findViewById(R.id.progress_text);
        dataText = (TextView) findViewById(R.id.data_text);
        dataField = (TextView) findViewById(R.id.data_value);

        device_address.setText(deviceAddress);
        getActionBar().setTitle(deviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        final Intent gattServiceIntent = new Intent(this, BleService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        System.out.println(newConfig.toString());
        super.onConfigurationChanged(newConfig);
    }

    //js interactive
    final class GuestureJavaScriptInterface {

        GuestureJavaScriptInterface() {

        }

        @JavascriptInterface
        public void recenter() {
            Toast.makeText(DeviceServicesActivity.this, "Re-center", Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public float getQuaternionW()
        {
            return q.w;
        }
        @JavascriptInterface
        public float getQuaternionX()
        {
            return q.x;
        }
        @JavascriptInterface
        public float getQuaternionY()
        {
            return q.y;
        }
        @JavascriptInterface
        public float getQuaternionZ()
        {
            return q.z;
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        unregisterReceiver(gattUpdateReceiver);
        bleService = null;
        destroyWebView();
    }

    public void destroyWebView() {
        if(wv_guesture != null) {
            wv_guesture.clearHistory();
            wv_guesture.clearCache(true);
            wv_guesture.loadUrl("about:blank"); // clearView() should be changed to loadUrl("about:blank"), since clearView() is deprecated now
            wv_guesture.freeMemory();
            wv_guesture.removeAllViews();
            wv_guesture = null; // Note that mWebView.destroy() and mWebView = null do the exact same thing
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String file_path = ContentUriUtil.getPath(this, uri);
            Log.d(TAG, file_path);

            bleService.notifyConfig(gForceOadService.UUID_SERVICE, gForceOadService.UUID_IMG_IDENTIFY, TRUE);
            Log.d(TAG, "Enable OAD image identity char notify");

            bleService.notifyConfig(gForceOadService.UUID_SERVICE, gForceOadService.UUID_IMG_BLOCK, TRUE);
            Log.d(TAG, "Enable OAD image block char notify");

            try {
                // Read firmware image file from device's local storage.
                imageData = readImageFile(file_path);
                byte[] blockBuf = readBlock(blockNum, imageData);

                Log.d(TAG, "imageData Len and header: " + imageData.length);
                dumpBytes(blockBuf);
                // Write OAD Image Identify Characteristic.
                bleService.write(oadSrv.getUuid().toString(),
                        oadImgIdentifyChar.getUuid().toString(),
                        blockBuf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Dump contents in byte array, use hex format.
    public void dumpBytes(byte[] inputBytes) {
        if (inputBytes != null && inputBytes.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(inputBytes.length);
            for (byte byteChar : inputBytes) {
                stringBuilder.append(String.format("%02X ", byteChar));
            }
            Log.d(TAG, stringBuilder.toString());
        }
    }

    // add block number for OAD block data, 2 bytes of block number + 16 bytes of block data
    public byte[] addBlknumToBlock(byte[] blkNumBytes, byte[] blockBytes) {
        byte[] addBlknumBlkBytes = new byte[18];
        addBlknumBlkBytes[0] = blkNumBytes[0];
        addBlknumBlkBytes[1] = blkNumBytes[1];
        for (int i = 0; i < 16; i++) {
            addBlknumBlkBytes[i + 2] = blockBytes[i];
        }
        return addBlknumBlkBytes;
    }

    // read a block data(16 bytes) from image file
    public byte[] readBlock(int blockNumber, byte[] firmwareImageData) {
        byte[] blockData = new byte[16];
        for (int i = 0; i < 16; i++) {
            blockData[i] = firmwareImageData[i + blockNumber * 16];
        }
        return blockData;
    }

    // read file to memory
    public byte[] readImageFile(String fileName) throws IOException {

        File file = new File(fileName);

        FileInputStream fis = new FileInputStream(file);

        int length = fis.available();

        Log.d(TAG, "Binary File Len: " + length);

        byte[] buffer = new byte[length];
        fis.read(buffer);

        fis.close();
        return buffer;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (isConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                bleService.connect(deviceAddress);
                dialog.setContent("Connecting...");
                dialog.show();
                return true;
            case R.id.menu_disconnect:
                bleService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionState.setText(resourceId);
            }
        });
    }

    // Display notification data in data file text view.
    private void displayData(byte[] data) {
        changeTextCorlor(1);

        if (data == null) {
            return;
        }

        // Get a GForceData object from a legal data
        GForceData gForceData = new GForceData.Builder(data).build();
        if (gForceData == null) {
            return;
        }

        // Calculate FPS
        long time = System.currentTimeMillis();
        if (mPackageCount == -1) {
            mPackageCount = 1;
            mPreviousTime = time;
        } else {
            ++mPackageCount;
            long timeElapsed = time - mPreviousTime;
            if (timeElapsed >= 1000) {
                Log.i(TAG, String.format("FPS: %d", mPackageCount * 1000 / timeElapsed));
                mPreviousTime = time;
                mPackageCount = 0;
            }
        }
        // check the lost package
        Byte package_id = gForceData.getPackageId();
        if (package_id != null) {
            if (mLastPackageID != -1) {
                int lostPackageCount = package_id - mLastPackageID;
                if (lostPackageCount > 1) {
                    mTotallyLostPackageCount += lostPackageCount;
                    Log.e(TAG, String.format("Lost packages: %d last second, totally %d ", lostPackageCount, mTotallyLostPackageCount));
                }
            }
            mLastPackageID = package_id;
        }

        // Display the data
        final StringBuilder stringBuilder = new StringBuilder();
        int type = gForceData.getType();
        if (type == GForceData.QUATERNION_FLOAT) {
            q = gForceData.getQuaternion();

            stringBuilder.append(String.format("w: %f\n", q.w));
            stringBuilder.append(String.format("x: %f\n", q.x));
            stringBuilder.append(String.format("y: %f\n", q.y));
            stringBuilder.append(String.format("z: %f", q.z));

            dataField.setText(stringBuilder.toString());
            //update the quaternion
            wv_guesture.loadUrl("javascript:funUpdateQa()");

        } else if (type == GForceData.GESTURE) {
            String gestureName = gForceData.getGestureName();
            if (gestureName == null) {
                Log.e(TAG, String.format("Illegal gesture: %d", gForceData.getGesture()));
            }
            stringBuilder.append(String.format("    %s", gestureName));
//            if (mPreviousGestureName != null) {
//                stringBuilder.append(String.format("    %s (previous)\n", mPreviousGestureName));
//            }
//            mPreviousGestureName = gestureName;
            gestureField.setText(stringBuilder.toString());
            diaplayGuestureAction(gestureName);
        } else if (type == GForceData.STATUS_UPDATE) {
            int status = gForceData.getStatusUpdate();
            if ((status & GForceData.STATUS_UPDATE_BASE_COORD_FRAME_SYNCHRONIZED) != 0) {
                Toast.makeText(this, "The pose base coordicate frame was synchronized!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void changeTextCorlor(int i) {
        if (i==1){
            wv_guesture.setVisibility(View.VISIBLE);
            dataText.setVisibility(View.GONE);
            tv_service_title.setVisibility(View.INVISIBLE);
            gattServicesList.setVisibility(View.INVISIBLE);
            findViewById(R.id.ll_connection).setVisibility(View.GONE);
            findViewById(R.id.ll_device).setVisibility(View.GONE);
            findViewById(R.id.ll_data).setPadding(0,0,0,0);
            findViewById(R.id.ll_guesture).setPadding(0,0,0,0);

            dataField.setTextColor(Color.WHITE);
            dataField.setTextSize(12);
            gestureText.setTextColor(Color.WHITE);
            gestureText.setTextSize(12);
            gestureField.setTextColor(Color.WHITE);
            gestureField.setTextSize(12);
        }else {
            wv_guesture.setVisibility(View.GONE);
            dataText.setVisibility(View.VISIBLE);
            tv_service_title.setVisibility(View.VISIBLE);
            gattServicesList.setVisibility(View.VISIBLE);
            findViewById(R.id.ll_connection).setVisibility(View.VISIBLE);
            findViewById(R.id.ll_device).setVisibility(View.VISIBLE);
            findViewById(R.id.ll_data).setPadding(30,0,0,0);
            findViewById(R.id.ll_guesture).setPadding(30,0,0,0);

            dataField.setTextSize(18);
            gestureText.setTextSize(18);
            gestureField.setTextSize(18);

            dataField.setTextColor(Color.BLACK);
            gestureText.setTextColor(Color.BLACK);
            gestureField.setTextColor(Color.BLACK);
        }
    }

    //display the guesture action on hands model
    private void diaplayGuestureAction(String gestureName) {
        switch (gestureName){
            case "GESTURE_RELAX":
                wv_guesture.loadUrl("javascript:funRelax()");
                break;
            case "GESTURE_GIST":
                wv_guesture.loadUrl("javascript:funFist()");
                break;
            case "GESTURE_SPREAD_FINGERS":
                wv_guesture.loadUrl("javascript:funSpread()");
                break;
            case "GESTURE_WAVE_TOWARD_PALM":
                wv_guesture.loadUrl("javascript:funWaveIn()");
                break;
            case "GESTURE_WAVE_BACKWARD_PALM":
                wv_guesture.loadUrl("javascript:funWaveOut()");
                break;
            case "GESTURE_SHOOT":
                wv_guesture.loadUrl("javascript:funShoot()");
                break;
            case "GESTURE_TUCK_FINGERS":
                wv_guesture.loadUrl("javascript:funPinch()");
                break;
            case "GESTURE_UNKNOWN":
                Toast.makeText(this,"GESTURE_UNKNOWN",Toast.LENGTH_SHORT);
                break;
        }
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;

        gattServiceAdapter = new BleServicesAdapter(this, gattServices);
        gattServiceAdapter.setServiceListener(demoClickListener);
        gattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
