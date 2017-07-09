package com.oymotion.gforcedev.info_service;

import java.util.HashMap;

/** a hash map to collect all of base information related services.
 *  Modified by Ethan.
 */
public class BleInfoServices {

    private static HashMap<String, BleInfoService> SERVICES = new HashMap<String, BleInfoService>();

    static {
		final BleGapService gapSerivce = new BleGapService();
        final BleGattService gattSerivce = new BleGattService();
        final BleDeviceInfoService deviceInfoSerivce = new BleDeviceInfoService();

        SERVICES.put(gapSerivce.getUUID(), gapSerivce);
        SERVICES.put(gattSerivce.getUUID(), gattSerivce);
        SERVICES.put(deviceInfoSerivce.getUUID(), deviceInfoSerivce);
    }

    public static BleInfoService getService(String uuid) {
        return SERVICES.get(uuid);
    }
}
