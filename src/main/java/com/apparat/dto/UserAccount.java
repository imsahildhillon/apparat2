package com.apparat.dto;

import com.apparat.model.Role;

/**
 * A user's account details without the password hash.
 */
public record UserAccount(
        int id,
        String name,
        String email,
        Role role,
        boolean active
) {}
