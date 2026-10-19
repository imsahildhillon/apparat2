package com.apparat.model;

public class UserTests {
    public static void main(String[] args){
        // Test 1: Valid user
        try {
            User goodUser = new User(
                    1, "Sahil", "sahil@example.com", Role.STUDENT
            );

            System.out.println("Good user created successfully!");

        } catch (IllegalArgumentException e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }

        // Test 2: Invalid user with null name
        User badUser = new User(
                2, null, "bad@example.com", Role.FACULTY
        );

        System.out.println("This line will not execute.");
    }

}
