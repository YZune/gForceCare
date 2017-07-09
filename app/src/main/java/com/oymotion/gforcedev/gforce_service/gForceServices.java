package com.oymotion.gforcedev.gforce_service;

import java.util.HashMap;

/** a hash map to collect all of gforce device related services.
 *  Created by Ethan on 2016/12/26 0003.
 */

public class gForceServices {
    private static HashMap<String, gForceService> SERVICES = new HashMap<String, gForceService>();

    static {
        final gForceDataService gforceDataService = new gForceDataService();
        final gForceOadResetService gforceOadResetService = new gForceOadResetService();
        final gForceOadService gforceOadService = new gForceOadService();

        SERVICES.put(gforceDataService.getUUID(), gforceDataService);
        SERVICES.put(gforceOadResetService.getUUID(), gforceOadResetService);
        SERVICES.put(gforceOadService.getUUID(), gforceOadService);
    }

    public static gForceService getService(String uuid) {
        return SERVICES.get(uuid);
    }
}
