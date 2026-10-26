package com.apparat.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The one place that says what each role may do.
 * Pages, menus and services all ask Role.can(...) instead of comparing roles.
 */
public enum Role {
    STUDENT("Student", EnumSet.noneOf(Permission.class)),

    FACULTY("Faculty", EnumSet.noneOf(Permission.class)),

    TECHNICIAN("Technician", EnumSet.of(
            Permission.MANAGE_RESOURCES,
            Permission.MANAGE_MAINTENANCE,
            Permission.VIEW_REPORTS
    )),

    ADMIN("Administrator", EnumSet.allOf(Permission.class));

    private final String label;
    private final Set<Permission> permissions;

    Role(String label, Set<Permission> permissions) {
        this.label = label;
        this.permissions = permissions;
    }

    public boolean can(Permission permission) {
        return permissions.contains(permission);
    }

    public String getLabel() {
        return label;
    }

    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }
}
