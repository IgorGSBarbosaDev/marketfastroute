package com.marketfastroute.routing;

import java.util.UUID;

public class RoutePathNotFoundException extends RuntimeException {

    private final UUID startNodeId;
    private final UUID destinationNodeId;

    public RoutePathNotFoundException(UUID startNodeId, UUID destinationNodeId) {
        super("No route path between nodes: " + startNodeId + " and " + destinationNodeId);
        this.startNodeId = startNodeId;
        this.destinationNodeId = destinationNodeId;
    }

    public UUID getStartNodeId() {
        return startNodeId;
    }

    public UUID getDestinationNodeId() {
        return destinationNodeId;
    }
}
