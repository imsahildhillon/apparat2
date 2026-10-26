package com.apparat.servlet;

import com.apparat.dto.SessionUser;
import com.apparat.filter.SessionSupport;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Parent of every servlet that needs to know who is logged in.
 */
public abstract class BaseServlet extends HttpServlet {

    /**
     * The logged-in user. AuthFilter has already turned away visitors who
     * are not logged in, so a missing user here means a programming mistake.
     */
    protected SessionUser currentUser(HttpServletRequest request) {

        SessionUser user = SessionSupport.current(request.getSession(false));

        if (user == null) {
            throw new IllegalStateException(
                    "No user is logged in; AuthFilter should have stopped "
                            + "this request."
            );
        }

        return user;
    }
}
