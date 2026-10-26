package com.apparat.dto;

import com.apparat.model.Role;

/**
 * What the HTTP session remembers about the logged-in user.
 * It never contains the password hash.
 */
public record SessionUser(
        int id,
        String name,
        Role role
) {}
