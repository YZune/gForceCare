package com.oymotion.gforcedev.gforce_service;


/** abstract class for gforce device related services.
 *  Created by Ethan on 2016/12/26 0003.
 */
public abstract class gForceService {
    private final static String TAG = gForceService.class.getSimpleName();

    protected gForceService() {
    }

    public abstract String getUUID();

    public abstract String getName();

    public abstract String getCharacteristicName(String uuid);

    public abstract String getCharacteristicValue(String uuid);

    public abstract void setCharacteristicValue(String uuid, String valueStr);
}
