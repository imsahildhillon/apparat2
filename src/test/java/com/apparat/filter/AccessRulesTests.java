package com.apparat.filter;

import com.apparat.dto.SessionUser;
import com.apparat.filter.AccessRules.Decision;
import com.apparat.model.Role;

import org.junit.jupiter.api.Test;

import static com.apparat.filter.AccessRules.Decision.ALLOW;
import static com.apparat.filter.AccessRules.Decision.FORBIDDEN;
import static com.apparat.filter.AccessRules.Decision.LOGIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every role against every kind of URL, in one table.
 * Columns: student, faculty, technician, admin, and nobody logged in.
 */
class AccessRulesTests {


    // path, student, faculty, technician, admin, not logged in
    private static final Object[][] MATRIX = {
            // public pages
            {"/login",                 ALLOW, ALLOW, ALLOW, ALLOW, ALLOW},
            {"/css/app.css",           ALLOW, ALLOW, ALLOW, ALLOW, ALLOW},
            {"/js/app.js",             ALLOW, ALLOW, ALLOW, ALLOW, ALLOW},

            // pages every logged-in user may open
            {"/",                      ALLOW, ALLOW, ALLOW, ALLOW, LOGIN},
            {"/home",                  ALLOW, ALLOW, ALLOW, ALLOW, LOGIN},
            {"/resources",             ALLOW, ALLOW, ALLOW, ALLOW, LOGIN},
            {"/availability",          ALLOW, ALLOW, ALLOW, ALLOW, LOGIN},
            {"/book",                  ALLOW, ALLOW, ALLOW, ALLOW, LOGIN},
            {"/bookings",              ALLOW, ALLOW, ALLOW, ALLOW, LOGIN},
            {"/cancel",                ALLOW, ALLOW, ALLOW, ALLOW, LOGIN},
            {"/export/bookings",       ALLOW, ALLOW, ALLOW, ALLOW, LOGIN},
            {"/logout",                ALLOW, ALLOW, ALLOW, ALLOW, LOGIN},

            // instrument management
            {"/manage/resources",      FORBIDDEN, FORBIDDEN, ALLOW, ALLOW, LOGIN},
            {"/manage/import",         FORBIDDEN, FORBIDDEN, ALLOW, ALLOW, LOGIN},

            // maintenance windows
            {"/manage/maintenance",    FORBIDDEN, FORBIDDEN, ALLOW, ALLOW, LOGIN},

            // reports
            {"/reports",               FORBIDDEN, FORBIDDEN, ALLOW, ALLOW, LOGIN},

            // user management: administrators only
            {"/admin/users",           FORBIDDEN, FORBIDDEN, FORBIDDEN, ALLOW, LOGIN},

            // pages nobody has opened up yet stay closed to everyone but the admin
            {"/manage/anything-new",   FORBIDDEN, FORBIDDEN, FORBIDDEN, ALLOW, LOGIN},
            {"/manage",                FORBIDDEN, FORBIDDEN, FORBIDDEN, ALLOW, LOGIN},
            {"/admin/other",           FORBIDDEN, FORBIDDEN, FORBIDDEN, ALLOW, LOGIN},
            {"/admin",                 FORBIDDEN, FORBIDDEN, FORBIDDEN, ALLOW, LOGIN},
    };

    private static final Role[] ROLES =
            {Role.STUDENT, Role.FACULTY, Role.TECHNICIAN, Role.ADMIN};

    @Test
    void everyRoleGetsTheExpectedDecisionForEveryUrl() {
        for (Object[] row : MATRIX) {
            String path = (String) row[0];

            for (int i = 0; i < ROLES.length; i++) {
                SessionUser user = new SessionUser(1, "Test", ROLES[i]);

                assertEquals(row[i + 1], AccessRules.decide(path, user),
                        ROLES[i] + " opening " + path);
            }

            assertEquals(row[5], AccessRules.decide(path, null),
                    "nobody logged in opening " + path);
        }
    }

    @Test
    void aPathThatMerelyStartsWithManageIsNotTreatedAsAManagePage() {
        // /managers is an ordinary page, not part of /manage.
        SessionUser student = new SessionUser(1, "Test", Role.STUDENT);

        assertEquals(ALLOW, AccessRules.decide("/managers", student));
        assertEquals(ALLOW, AccessRules.decide("/administration", student));
    }

    @Test
    void theCheckLooksAtTheRoleNotTheName() {
        // Same name as an administrator, but a student's role.
        SessionUser fake = new SessionUser(99, "Demo Admin", Role.STUDENT);

        assertEquals(FORBIDDEN, AccessRules.decide("/admin/users", fake));
    }
}
