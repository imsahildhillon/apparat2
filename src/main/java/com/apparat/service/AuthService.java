package com.apparat.service;

import com.apparat.dao.UserDao;
import com.apparat.dto.SessionUser;
import com.apparat.dto.UserAccount;
import com.apparat.dto.UserCredentials;
import com.apparat.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {

    private static final String LOGIN_FAILED = "Invalid email or password.";

    // Verified against when the email is unknown, so an unknown email
    // takes about as long as a wrong password.
    private static final String DUMMY_HASH =
            PasswordUtil.hash("not-a-real-password");

    private final UserDao userDao = new UserDao();

    public SessionUser login(String email, String password)
            throws SQLException {

        // 1. Missing input gets the same generic message as a wrong login.
        if (email == null || email.isBlank()
                || password == null || password.isEmpty()) {
            throw new AuthenticationException(LOGIN_FAILED);
        }

        // 2. Look the user up by email.
        Optional<UserCredentials> userOptional =
                userDao.findCredentialsByEmail(email.trim());

        if (userOptional.isEmpty()) {
            PasswordUtil.verify(password, DUMMY_HASH);
            throw new AuthenticationException(LOGIN_FAILED);
        }

        UserCredentials user = userOptional.get();

        // 3. Compare the password with the stored salted hash.
        if (!PasswordUtil.verify(password, user.passwordHash())) {
            throw new AuthenticationException(LOGIN_FAILED);
        }

        // 4. A deactivated account cannot log in. This is said only after the
        //    password was right, so it does not reveal which emails exist.
        if (!user.active()) {
            throw new AuthenticationException(
                    "This account has been deactivated. "
                            + "Please contact an administrator."
            );
        }

        // 5. Only the safe fields go into the session.
        return new SessionUser(user.id(), user.name(), user.role());
    }

    /**
     * Re-reads the logged-in user from the database. Returns empty when the
     * account was deleted or deactivated; otherwise the current name and role,
     * so a role change takes effect on the user's very next request.
     */
    public Optional<SessionUser> refresh(SessionUser current)
            throws SQLException {

        return userDao.findAccountById(current.id())
                .filter(UserAccount::active)
                .map(account -> new SessionUser(
                        account.id(), account.name(), account.role()));
    }
}
