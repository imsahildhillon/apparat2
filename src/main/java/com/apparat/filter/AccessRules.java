package com.apparat.filter;

import com.apparat.dto.SessionUser;
import com.apparat.model.Permission;

/**
 * Which page needs which permission. Pure logic with no servlet objects,
 * so a test can check every role against every URL.
 */
public class AccessRules {

    public enum Decision {
        ALLOW,
        LOGIN,
        FORBIDDEN
    }

    private AccessRules() {
    }

    public static boolean isPublic(String path) {
        return path.equals("/login")
                || path.startsWith("/css/")
                || path.startsWith("/js/");
    }

    /**
     * The permission a path needs, or null when any logged-in user may open it.
     * Unlisted pages under /manage and /admin are administrator-only, so a
     * new page added there is closed until someone opens it on purpose.
     */
    public static Permission requiredPermission(String path) {

        if (under(path, "/manage/resources") || under(path, "/manage/import")) {
            return Permission.MANAGE_RESOURCES;
        }

        if (under(path, "/manage/maintenance")) {
            return Permission.MANAGE_MAINTENANCE;
        }

        if (under(path, "/reports")) {
            return Permission.VIEW_REPORTS;
        }

        if (under(path, "/admin/users")) {
            return Permission.MANAGE_USERS;
        }

        if (under(path, "/manage") || under(path, "/admin")) {
            return Permission.MANAGE_USERS;
        }

        return null;
    }

    public static Decision decide(String path, SessionUser user) {

        if (isPublic(path)) {
            return Decision.ALLOW;
        }

        if (user == null) {
            return Decision.LOGIN;
        }

        Permission needed = requiredPermission(path);

        if (needed != null && !user.role().can(needed)) {
            return Decision.FORBIDDEN;
        }

        return Decision.ALLOW;
    }

    // "/manage" matches /manage and /manage/x, but not /managers.
    private static boolean under(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
