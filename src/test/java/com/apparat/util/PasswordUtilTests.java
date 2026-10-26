package com.apparat.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTests {

    @Test
    void correctPasswordVerifies() {
        String stored = PasswordUtil.hash("Student@123");

        assertTrue(PasswordUtil.verify("Student@123", stored));
    }

    @Test
    void wrongPasswordIsRejected() {
        String stored = PasswordUtil.hash("Student@123");

        assertFalse(PasswordUtil.verify("student@123", stored));
        assertFalse(PasswordUtil.verify("", stored));
    }

    @Test
    void samePasswordGivesDifferentHashesBecauseOfSalt() {
        String first = PasswordUtil.hash("Student@123");
        String second = PasswordUtil.hash("Student@123");

        assertNotEquals(first, second);
        assertTrue(PasswordUtil.verify("Student@123", first));
        assertTrue(PasswordUtil.verify("Student@123", second));
    }

    @Test
    void storedHashDoesNotContainThePassword() {
        String stored = PasswordUtil.hash("Student@123");

        assertFalse(stored.contains("Student@123"));
        assertTrue(stored.startsWith("pbkdf2$"));
    }

    @Test
    void malformedStoredValuesNeverVerify() {
        assertFalse(PasswordUtil.verify("anything", ""));
        assertFalse(PasswordUtil.verify("anything", "plain-text-password"));
        assertFalse(PasswordUtil.verify("anything", "pbkdf2$abc$!!$!!"));
        assertFalse(PasswordUtil.verify("anything", null));
        assertFalse(PasswordUtil.verify(null, "pbkdf2$1$AA==$AA=="));
    }

    @Test
    void emptyPasswordCannotBeHashed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PasswordUtil.hash("")
        );
    }
}
