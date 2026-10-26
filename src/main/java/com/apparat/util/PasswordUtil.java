package com.apparat.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Salted PBKDF2 password hashing. A stored value looks like:
 * pbkdf2$iterations$saltBase64$hashBase64
 */
public class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String password) {

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                    "Password cannot be null or empty"
            );
        }

        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(
                password.toCharArray(), salt, ITERATIONS, HASH_BITS
        );

        return PREFIX + "$" + ITERATIONS
                + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(String password, String storedHash) {

        if (password == null || storedHash == null) {
            return false;
        }

        String[] parts = storedHash.split("\\$");

        if (parts.length != 4 || !parts[0].equals(PREFIX)) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);

            if (iterations <= 0 || expected.length == 0) {
                return false;
            }

            byte[] actual = pbkdf2(
                    password.toCharArray(),
                    salt,
                    iterations,
                    expected.length * 8
            );

            // Constant-time comparison, so timing does not leak anything.
            return MessageDigest.isEqual(expected, actual);

        } catch (IllegalArgumentException e) {
            // Malformed stored value (bad number or bad Base64).
            return false;
        }
    }

    private static byte[] pbkdf2(
            char[] password,
            byte[] salt,
            int iterations,
            int bits
    ) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password, salt, iterations, bits
            );

            return SecretKeyFactory.getInstance(ALGORITHM)
                    .generateSecret(spec)
                    .getEncoded();

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Password hashing is not available.", e
            );
        }
    }
}
