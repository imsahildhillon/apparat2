package com.apparat.model;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolePermissionTests {

    @Test
    void studentsAndFacultyHaveNoStaffPermissions() {
        for (Permission permission : Permission.values()) {
            assertFalse(Role.STUDENT.can(permission), permission.name());
            assertFalse(Role.FACULTY.can(permission), permission.name());
        }
    }

    @Test
    void technicianManagesInstrumentsMaintenanceAndReportsButNotUsers() {
        assertEquals(
                EnumSet.of(Permission.MANAGE_RESOURCES,
                        Permission.MANAGE_MAINTENANCE,
                        Permission.VIEW_REPORTS),
                Role.TECHNICIAN.getPermissions());

        assertFalse(Role.TECHNICIAN.can(Permission.MANAGE_USERS));
    }

    @Test
    void adminHasEveryPermission() {
        for (Permission permission : Permission.values()) {
            assertTrue(Role.ADMIN.can(permission), permission.name());
        }
    }

    @Test
    void adminCanDoEverythingATechnicianCan() {
        assertTrue(Role.ADMIN.getPermissions()
                .containsAll(Role.TECHNICIAN.getPermissions()));
    }

    @Test
    void onlyAdminManagesUsers() {
        for (Role role : Role.values()) {
            assertEquals(role == Role.ADMIN, role.can(Permission.MANAGE_USERS),
                    role.name());
        }
    }

    @Test
    void thePermissionSetCannotBeChangedFromOutside() {
        Set<Permission> permissions = Role.TECHNICIAN.getPermissions();

        assertThrows(UnsupportedOperationException.class,
                () -> permissions.add(Permission.MANAGE_USERS));
    }

    @Test
    void everyRoleHasALabelForScreens() {
        assertEquals("Student", Role.STUDENT.getLabel());
        assertEquals("Faculty", Role.FACULTY.getLabel());
        assertEquals("Technician", Role.TECHNICIAN.getLabel());
        assertEquals("Administrator", Role.ADMIN.getLabel());
    }
}
