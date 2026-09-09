package com.marketfastroute.routing;

import java.util.UUID;

public class RoutePointNotFoundException extends RuntimeException {

    private final String pointType;

    public RoutePointNotFoundException(UUID mapId, String pointType) {
        super("No active navigable " + pointType + " configured for map: " + mapId);
        this.pointType = pointType;
    }

    public String getPointType() {
        return pointType;
    }
}
