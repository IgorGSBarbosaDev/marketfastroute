package com.marketfastroute.admin;

public class AdminResourceNotFoundException extends RuntimeException {

    private final String resource;

    public AdminResourceNotFoundException(String resource) {
        super(resource + " not found");
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
