package com.marketfastroute.routing;

import java.util.UUID;

public interface PathFinder {

    PathResult findShortestPath(
            NavigationGraph graph,
            UUID startNodeId,
            UUID destinationNodeId
    );
}
