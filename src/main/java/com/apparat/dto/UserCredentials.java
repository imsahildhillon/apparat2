package com.apparat.dto;

import com.apparat.model.Role;

/**
 * A user row including the stored password hash.
 * Only used while checking a login, then discarded.
 */
public record UserCredentials(
        int id,
        String name,
        String email,
        Role role,
        String passwordHash,
        boolean active
) {}
