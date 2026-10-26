package com.apparat.servlet;

import com.apparat.dto.SessionUser;
import com.apparat.model.Role;
import com.apparat.service.DuplicateUserException;
import com.apparat.service.UserAdminService;
import com.apparat.service.UserNotFoundException;
import com.apparat.service.UserRuleException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * User management for administrators. AuthFilter restricts every /admin URL
 * to roles holding the MANAGE_USERS permission.
 */
@WebServlet("/admin/users")
public class AdminUsersServlet extends BaseServlet {

    private final UserAdminService userAdminService = new UserAdminService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        // Show and consume the one-time message left by a previous POST.
        HttpSession session = request.getSession(false);

        if (session != null) {
            Object message = session.getAttribute("adminMessage");

            if (message != null) {
                request.setAttribute("adminMessage", message);
                session.removeAttribute("adminMessage");
            }
        }

        showPage(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        SessionUser admin = currentUser(request);
        String action = request.getParameter("action");

        // Keep what was typed in the add form. The password is never sent back.
        request.setAttribute("newName", request.getParameter("name"));
        request.setAttribute("newEmail", request.getParameter("email"));
        request.setAttribute("newRole", request.getParameter("role"));

        try {
            if ("create".equals(action)) {
                userAdminService.createUser(
                        request.getParameter("name"),
                        request.getParameter("email"),
                        request.getParameter("role"),
                        request.getParameter("password")
                );
                flash(request, "User created.");

            } else if ("role".equals(action)) {
                userAdminService.changeRole(
                        admin.id(),
                        Integer.parseInt(request.getParameter("id")),
                        parseRole(request.getParameter("role"))
                );
                flash(request, "Role changed. It applies on the user's "
                        + "next page.");

            } else if ("password".equals(action)) {
                userAdminService.resetPassword(
                        Integer.parseInt(request.getParameter("id")),
                        request.getParameter("password")
                );
                flash(request, "Password changed.");

            } else if ("active".equals(action)) {
                boolean active = "true".equals(request.getParameter("active"));

                userAdminService.setActive(
                        admin.id(),
                        Integer.parseInt(request.getParameter("id")),
                        active
                );
                flash(request, active
                        ? "Account activated."
                        : "Account deactivated. The user is logged out.");

            } else {
                throw new IllegalArgumentException("Unknown action.");
            }

            // Redirect after a successful POST, so a refresh cannot repeat it.
            response.sendRedirect(request.getContextPath() + "/admin/users");

        } catch (DuplicateUserException | UserRuleException e) {
            fail(request, response, HttpServletResponse.SC_CONFLICT,
                    e.getMessage());

        } catch (UserNotFoundException e) {
            fail(request, response, HttpServletResponse.SC_NOT_FOUND,
                    e.getMessage());

        } catch (NumberFormatException e) {
            fail(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "Please check your inputs.");

        } catch (IllegalArgumentException e) {
            fail(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage());

        } catch (SQLException e) {
            throw new ServletException("Unable to update users.", e);
        }
    }

    // Null for text that is not a role, so the service can explain it.
    private Role parseRole(String text) {
        try {
            return text == null ? null : Role.valueOf(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void flash(HttpServletRequest request, String message) {
        request.getSession().setAttribute("adminMessage", message);
    }

    private void fail(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String message
    ) throws ServletException, IOException {

        response.setStatus(status);
        request.setAttribute("error", message);
        showPage(request, response);
    }

    private void showPage(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        try {
            request.setAttribute("users", userAdminService.listUsers());
            request.setAttribute("roles", Role.values());
            request.setAttribute("myId", currentUser(request).id());

            request.getRequestDispatcher(
                    "/WEB-INF/views/manage-users.jsp"
            ).forward(request, response);

        } catch (SQLException e) {
            throw new ServletException("Unable to load users.", e);
        }
    }
}
