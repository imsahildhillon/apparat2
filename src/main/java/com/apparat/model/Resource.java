package com.apparat.model;

public class Resource {
    private int id;
    private String name;
    private String location;
    private ResourceStatus status;

    public Resource(int id,String name,String location, ResourceStatus status) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Location cannot be null or blank");
        }

        if (status == null ) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        this.id = id;
        this.name = name;
        this.location=location;
        this.status = status;
    }

    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getLocation() {
        return location;
    }
    public ResourceStatus getStatus() {
        return status;
    }


}

