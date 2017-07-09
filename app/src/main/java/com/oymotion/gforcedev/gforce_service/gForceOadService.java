package com.oymotion.gforcedev.gforce_service;

import java.util.HashMap;

/** gforce OAD service, used only in OAD mode.
 *  Created by Ethan on 2017/1/4 0004.
 */
public class gForceOadService extends gForceService {
    private final static String TAG = gForceOadService.class.getSimpleName();

    protected gForceOadService() {
    }

    public static final String UUID_SERVICE = "f000ffc0-0451-4000-b000-000000000000";

    public static final String UUID_IMG_IDENTIFY = "f000ffc1-0451-4000-b000-000000000000";

    public static final String UUID_IMG_BLOCK = "f000ffc2-0451-4000-b000-000000000000";

    private static final HashMap<String, String> CHARACTERISTIC_MAP = new HashMap<String, String>();
    private static HashMap<String, String> CHARACTERISTIC_VAL_MAP = new HashMap<String, String>();

    static {
        CHARACTERISTIC_MAP.put(UUID_IMG_IDENTIFY, "Image Identify");
        CHARACTERISTIC_MAP.put(UUID_IMG_BLOCK, "Image Block");
    }

    static {
        CHARACTERISTIC_VAL_MAP.put(UUID_IMG_IDENTIFY, "Click to start upgrade");
        CHARACTERISTIC_VAL_MAP.put(UUID_IMG_BLOCK, null);
    }

    @Override
    public String getUUID() {
        return UUID_SERVICE;
    }

    @Override
    public String getName() {
        return "OAD Service";
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
