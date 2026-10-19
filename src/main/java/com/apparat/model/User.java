package com.apparat.model;

public class User {
    private int id;
    private String name;
    private String email;
    private Role role;

    public User(int id,String name,String email, Role role) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }

        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }
    public Role getRole() {
        return role;
    }

}
