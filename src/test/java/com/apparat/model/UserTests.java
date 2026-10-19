package com.apparat.model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTests{

    @Test
    void shouldCreateUserWithValidDetails() {
        User user = new User(
                1,
                "Sahil",
                "sahil@example.com",
                Role.STUDENT
        );

        assertEquals(1, user.getId());
        assertEquals("Sahil", user.getName());
        assertEquals("sahil@example.com", user.getEmail());
        assertEquals(Role.STUDENT, user.getRole());
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        IllegalArgumentException exception=assertThrows(
                IllegalArgumentException.class,
                () -> new User(
                        2,
                        "   ",
                        "bad@example.com",
                        Role.FACULTY
                )
        );
        assertEquals(
                "Name cannot be null or blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenEmailIsNull(){
        assertThrows(
                IllegalArgumentException.class,
                () -> new User(
                        3,
                        "Raj",
                        null,
                        Role.STUDENT
                )
        );
    }
    @Test
    void shouldThrowExceptionWhenRoleIsNull(){
        assertThrows(
                IllegalArgumentException.class,
                () -> new User(
                        4,
                        "Rahul",
                        "rahul@gmail.com",
                        null
                )
        );
    }
}