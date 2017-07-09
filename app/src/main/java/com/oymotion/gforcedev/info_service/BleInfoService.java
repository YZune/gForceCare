package com.oymotion.gforcedev.info_service;

/** abstract class of BLE base information related services.
 *  Modified by Ethan.
 */
public abstract class BleInfoService {
    private final static String TAG = BleInfoService.class.getSimpleName();

    protected BleInfoService() {
    }

    public abstract String getUUID();

    public abstract String getName();

    public abstract String getCharacteristicName(String uuid);

    public abstract String getCharacteristicValue(String uuid);

    public abstract void setCharacteristicValue(String uuid, String valueStr);
}
