package com.apparat.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ResourceTests {
    @Test
    public void testConstructor() {
        Resource resource =new Resource(
                101,
                "Photo-Scope",
                "PRP-711",
                ResourceStatus.ACTIVE
        );
        assertEquals(101,resource.getId());
        assertEquals("Photo-Scope",resource.getName());
    }

    @Test
    public void shouldThrowExceptionWhenNameIsBlank(){
        IllegalArgumentException exception= assertThrows(
                IllegalArgumentException.class,
                () -> new Resource(
                102,
                "   ",
                "PRP-711",
                ResourceStatus.ACTIVE
            )
        );

        assertEquals("Name cannot be null or blank",exception.getMessage());
    }



}
