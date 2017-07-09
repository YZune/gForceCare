package com.oymotion.gforcedev.info_service;

import java.util.HashMap;

/** GAP service.
 *  Modified by Ethan.
 */
public class BleGapService extends BleInfoService {

    public static final String UUID_SERVICE = "00001800-0000-1000-8000-00805f9b34fb";

    public static final String UUID_DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb";
    public static final String UUID_APPEARANCE = "00002a01-0000-1000-8000-00805f9b34fb";
    public static final String UUID_PPF = "00002a02-0000-1000-8000-00805f9b34fb";
    public static final String UUID_RECCONECTION_ADDRESS = "00002a03-0000-1000-8000-00805f9b34fb";
    public static final String UUID_PPCP = "00002a04-0000-1000-8000-00805f9b34fb";

    private static final HashMap<String, String> CHARACTERISTIC_MAP = new HashMap<String, String>();
    private static HashMap<String, String> CHARACTERISTIC_VAL_MAP = new HashMap<String, String>();

    static {
        CHARACTERISTIC_MAP.put(UUID_DEVICE_NAME, "Device name");
        CHARACTERISTIC_MAP.put(UUID_APPEARANCE, "Appearance");
        CHARACTERISTIC_MAP.put(UUID_PPF, "Peripheral Privacy Flag");
        CHARACTERISTIC_MAP.put(UUID_RECCONECTION_ADDRESS, "Reconnection address");
        CHARACTERISTIC_MAP.put(UUID_PPCP, "Peripheral Preferred Connection Parameters");
    }

    static {
        CHARACTERISTIC_VAL_MAP.put(UUID_DEVICE_NAME, null);
        CHARACTERISTIC_VAL_MAP.put(UUID_APPEARANCE, null);
        CHARACTERISTIC_VAL_MAP.put(UUID_PPCP, null);
    }

    @Override
    public String getUUID() {
        return UUID_SERVICE;
    }

    @Override
    public String getName() {
        return "GAP Service";
    }

    @Override
    public String getCharacteristicName(String uuid) {
        if (!CHARACTERISTIC_MAP.containsKey(uuid))
            return "Unknown";
        return CHARACTERISTIC_MAP.get(uuid);
    }

    @Override
    public String getCharacteristicValue(String uuid) {
        if (!CHARACTERISTIC_VAL_MAP.containsKey(uuid))
            return null;
        return CHARACTERISTIC_VAL_MAP.get(uuid);
    }

    @Override
    public void setCharacteristicValue(String uuid, String valueStr) {
        if (CHARACTERISTIC_VAL_MAP.containsKey(uuid))
            CHARACTERISTIC_VAL_MAP.put(uuid, valueStr);
    }
}
