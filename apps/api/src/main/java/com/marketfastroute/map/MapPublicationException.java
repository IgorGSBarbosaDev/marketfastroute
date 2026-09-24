package com.marketfastroute.map;

import java.util.List;

public class MapPublicationException extends RuntimeException {

    private final List<MapPublicationIssue> issues;

    public MapPublicationException(List<MapPublicationIssue> issues) {
        super("Map is not ready for publication");
        this.issues = List.copyOf(issues);
    }

    public List<MapPublicationIssue> getIssues() {
        return issues;
    }
}
