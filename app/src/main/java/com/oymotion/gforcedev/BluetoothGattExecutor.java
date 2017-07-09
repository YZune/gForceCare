package com.oymotion.gforcedev;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import java.util.LinkedList;
import java.util.UUID;
import android.util.Log;

/**
 * GATT queue to manage all of the related commands and events to execute in order.
 * Modified by Ethan on 12/28/2016.
 */
public class BluetoothGattExecutor extends BluetoothGattCallback {
    private final static String TAG = BluetoothGattExecutor.class.getSimpleName();

    // Constants
    private static String CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public interface ServiceAction {
        public static final ServiceAction NULL = new ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                // it is null action. do nothing.
                return true;
            }
        };

        /***
         * Executes action.
         * @param bluetoothGatt
         * @return true - if action was executed instantly. false if action is waiting for
         *         feedback.
         */
        public boolean execute(BluetoothGatt bluetoothGatt);
    }

    private final LinkedList<BluetoothGattExecutor.ServiceAction> queue = new LinkedList<ServiceAction>();
    private volatile ServiceAction currentAction;

    protected void readChar(final String srvUuidStr, final String charUuidStr, final String descUuidStr) {
        ServiceAction action = new BluetoothGattExecutor.ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                final UUID srvUuid = UUID.fromString(srvUuidStr);
                final UUID charUuid = UUID.fromString(charUuidStr);
                final BluetoothGattService service = bluetoothGatt.getService(srvUuid);
                final BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUuid);
                if (characteristic != null) {
                    if (descUuidStr == null) {
                        // Read Characteristic
                        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                            bluetoothGatt.readCharacteristic(characteristic);
                            Log.d(TAG, "readChar");
                            return false;
                        } else {
                            Log.w(TAG, "read: characteristic not readable: " + charUuidStr);
                            return true;
                        }
                    } else {
                        // Read Descriptor
                        final UUID descUuid = UUID.fromString(descUuidStr);
                        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descUuid);
                        if (descriptor != null) {
                            bluetoothGatt.readDescriptor(descriptor);
                            return false;
                        } else {
                            Log.w(TAG, "read: descriptor not found: " + descUuidStr);
                            return true;
                        }
                    }
                } else {
                    Log.w(TAG, "read: characteristic not found: " + charUuidStr);
                    return true;
                }
            }
        };
        queue.add(action);
    }

    protected void writeChar(final String srvUuidStr, final String charUuidStr, final byte[] value) {
        ServiceAction action = new BluetoothGattExecutor.ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                final UUID srvUuid = UUID.fromString(srvUuidStr);
                final BluetoothGattService service = bluetoothGatt.getService(srvUuid);
                final UUID charUuid = UUID.fromString(charUuidStr);
                final BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUuid);
                if (characteristic != null) {
                    characteristic.setValue(value);
                    bluetoothGatt.writeCharacteristic(characteristic);
                    Log.d(TAG, "writeChar");
                    return false;
                } else {
                    Log.w(TAG, "write: characteristic not found: " + charUuid);
                    return true;
                }
            }
        };
        queue.add(action);
    }

    protected void enableNotify(final String srvUuidStr, final String charUuidStr, final boolean enable) {
        ServiceAction action = new BluetoothGattExecutor.ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                if (charUuidStr != null) {
                    final UUID srvUuid = UUID.fromString(srvUuidStr);
                    final BluetoothGattService service = bluetoothGatt.getService(srvUuid);
                    final UUID charUuid = UUID.fromString(charUuidStr);
                    final BluetoothGattCharacteristic dataCharacteristic = service.getCharacteristic(charUuid);

                    if (dataCharacteristic == null) {
                        Log.w(TAG, "Characteristic with UUID " + charUuidStr + " not found");
                        return true;
                    }

                    final UUID cccdUuid = UUID.fromString(CHARACTERISTIC_CONFIG);
                    final BluetoothGattDescriptor config = dataCharacteristic.getDescriptor(cccdUuid);
                    if (config == null)
                        return true;

                    // enableNotification/disable locally
                    bluetoothGatt.setCharacteristicNotification(dataCharacteristic, enable);
                    // enableNotification/disable remotely
                    config.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    bluetoothGatt.writeDescriptor(config);

                    return false;
                } else {
                    Log.w(TAG, "Characteristic UUID is null");
                    return true;
                }
            }
        };
        queue.add(action);
    }

    protected void enableIndicate(final String srvUuidStr, final String charUuidStr, final boolean enable) {
        ServiceAction action = new BluetoothGattExecutor.ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                if (charUuidStr != null) {
                    final UUID srvUuid = UUID.fromString(srvUuidStr);
                    final BluetoothGattService service = bluetoothGatt.getService(srvUuid);
                    final UUID charUuid = UUID.fromString(charUuidStr);
                    final BluetoothGattCharacteristic dataCharacteristic = service.getCharacteristic(charUuid);

                    if (dataCharacteristic == null) {
                        Log.w(TAG, "Characteristic with UUID " + charUuidStr + " not found");
                        return true;
                    }

                    final UUID cccdUuid = UUID.fromString(CHARACTERISTIC_CONFIG);
                    final BluetoothGattDescriptor config = dataCharacteristic.getDescriptor(cccdUuid);
                    if (config == null)
                        return true;

                    // enableNotification/disable remotely
                    config.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    bluetoothGatt.writeDescriptor(config);

                    return false;
                } else {
                    Log.w(TAG, "Characteristic UUID is null");
                    return true;
                }
            }
        };
        queue.add(action);
    }

    public void execute(BluetoothGatt gatt) {
        if (currentAction != null)
            return;

        boolean next = !queue.isEmpty();
        while (next) {
            final BluetoothGattExecutor.ServiceAction action = queue.pop();
            currentAction = action;
            if (!action.execute(gatt))
                break;

            currentAction = null;
            next = !queue.isEmpty();
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);

        currentAction = null;
        execute(gatt);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        currentAction = null;
        execute(gatt);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            queue.clear();
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic,
                                     int status) {
        currentAction = null;
        execute(gatt);
    }
}
