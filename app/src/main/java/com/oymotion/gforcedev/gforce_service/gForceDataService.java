package com.oymotion.gforcedev.gforce_service;

import java.util.HashMap;

/** gForce data and gForce control service.
 *  Created by Ethan on 2016/12/26 0003.
 */
public class gForceDataService extends gForceService {
    private final static String TAG = gForceDataService.class.getSimpleName();

    protected gForceDataService() {
    }

    public static final String UUID_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";

    public static final String UUID_GFORCE_DATA = "0000fff4-0000-1000-8000-00805f9b34fb";

    private static final HashMap<String, String> CHARACTERISTIC_MAP = new HashMap<String, String>();
    private static HashMap<String, String> CHARACTERISTIC_VAL_MAP = new HashMap<String, String>();

    static {
        CHARACTERISTIC_MAP.put(UUID_GFORCE_DATA, "gForce Data");
    }

    static {
        CHARACTERISTIC_VAL_MAP.put(UUID_GFORCE_DATA, "Start notify");
    }

    @Override
    public String getUUID() {
        return UUID_SERVICE;
    }

    @Override
    public String getName() {
        return "gForce Data";
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
