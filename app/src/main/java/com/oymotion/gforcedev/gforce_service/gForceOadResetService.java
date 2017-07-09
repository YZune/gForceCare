package com.oymotion.gforcedev.gforce_service;

import java.util.HashMap;

/** Reset gforce device's firmware, prepare for OAD download.
 *  Created by Ethan on 2016/12/26 0003.
 */
public class gForceOadResetService extends gForceService {
    private final static String TAG = gForceOadResetService.class.getSimpleName();

    protected gForceOadResetService() {
    }

    public static final String UUID_SERVICE = "f000ffd0-0451-4000-b000-000000000000";

    public static final String UUID_RESET = "f000ffd1-0451-4000-b000-000000000000";

    private static final HashMap<String, String> CHARACTERISTIC_MAP = new HashMap<String, String>();
    private static HashMap<String, String> CHARACTERISTIC_VAL_MAP = new HashMap<String, String>();

    static {
        CHARACTERISTIC_MAP.put(UUID_RESET, "Firmware Reset");
    }

    static {
        CHARACTERISTIC_VAL_MAP.put(UUID_RESET, "Click switch to OAD mode");
    }

    @Override
    public String getUUID() {
        return UUID_SERVICE;
    }

    @Override
    public String getName() {
        return "Firmware Upgrade";
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
