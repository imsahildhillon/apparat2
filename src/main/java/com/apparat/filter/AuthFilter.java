package com.apparat.filter;

import com.apparat.dto.SessionUser;
import com.apparat.service.AuthService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Runs before every request. The decisions themselves are in AccessRules;
 * this class only applies them.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    private final AuthService authService = new AuthService();

    @Override
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        HttpSession session = request.getSession(false);
        SessionUser user = SessionSupport.current(session);

        // Re-read the account on every page request, so a role change or a
        // deactivation by an administrator takes effect straight away.
        if (user != null && !AccessRules.isPublic(path)) {
            try {
                Optional<SessionUser> fresh = authService.refresh(user);

                if (fresh.isPresent()) {
                    user = fresh.get();
                    SessionSupport.store(session, user);
                } else {
                    session.invalidate();
                    user = null;
                }

            } catch (SQLException e) {
                throw new ServletException(
                        "Unable to check your account.", e
                );
            }
        }

        switch (AccessRules.decide(path, user)) {
            case LOGIN -> response.sendRedirect(
                    request.getContextPath() + "/login"
            );

            case FORBIDDEN -> response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "You do not have permission to open this page."
            );

            case ALLOW -> {
                // Keep logged-in pages out of the browser cache, so the Back
                // button after logout does not show them again.
                if (user != null) {
                    response.setHeader("Cache-Control", "no-store");
                }

                chain.doFilter(request, response);
            }
        }
    }
}
