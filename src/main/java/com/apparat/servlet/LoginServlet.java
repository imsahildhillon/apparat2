package com.apparat.servlet;

import com.apparat.dto.SessionUser;
import com.apparat.filter.SessionSupport;
import com.apparat.service.AuthService;
import com.apparat.service.AuthenticationException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        // Already logged in: go straight to the home page.
        HttpSession session = request.getSession(false);

        if (session != null && SessionSupport.current(session) != null) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }

        if ("1".equals(request.getParameter("loggedOut"))) {
            request.setAttribute("message", "You have been logged out.");
        }

        showForm(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            SessionUser user = authService.login(email, password);

            // Replace any old session with a new one, so a session id
            // planted before login cannot be reused (session fixation).
            HttpSession oldSession = request.getSession(false);

            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession session = request.getSession(true);
            SessionSupport.store(session, user);

            response.sendRedirect(request.getContextPath() + "/home");

        } catch (AuthenticationException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            request.setAttribute("error", e.getMessage());

            // Keep the email so the user does not retype it.
            // The password is never sent back to the page.
            request.setAttribute("email", email);
            showForm(request, response);

        } catch (SQLException e) {
            throw new ServletException("Unable to log in.", e);
        }
    }

    private void showForm(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.getRequestDispatcher(
                "/WEB-INF/views/login.jsp"
        ).forward(request, response);
    }
}
