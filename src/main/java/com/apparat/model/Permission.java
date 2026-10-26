package com.apparat.model;

/**
 * Things only some roles may do. Everything not listed here
 * (browsing, booking, cancelling one's own booking) is open to every role.
 */
public enum Permission {
    MANAGE_RESOURCES,
    MANAGE_MAINTENANCE,
    VIEW_REPORTS,
    MANAGE_USERS
}
