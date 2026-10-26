package com.apparat.filter;

import com.apparat.dto.SessionUser;
import com.apparat.model.Permission;

import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;

/**
 * Reads and writes the logged-in user in the HTTP session.
 */
public class SessionSupport {

    public static final String USER = "sessionUser";
    public static final String PERMISSIONS = "permissions";

    private SessionSupport() {
    }

    /**
     * Stores the user, plus a name-to-true/false map of their permissions so
     * a JSP can write ${permissions.MANAGE_RESOURCES}.
     */
    public static void store(HttpSession session, SessionUser user) {

        Map<String, Boolean> permissions = new HashMap<>();

        for (Permission permission : Permission.values()) {
            permissions.put(permission.name(), user.role().can(permission));
        }

        session.setAttribute(USER, user);
        session.setAttribute(PERMISSIONS, permissions);
    }

    /**
     * The logged-in user, or null when nobody is logged in.
     */
    public static SessionUser current(HttpSession session) {

        if (session == null) {
            return null;
        }

        return (SessionUser) session.getAttribute(USER);
    }
}
