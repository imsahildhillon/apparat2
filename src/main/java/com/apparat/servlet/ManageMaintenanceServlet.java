package com.apparat.servlet;

import com.apparat.dto.SessionUser;
import com.apparat.service.MaintenanceConflictException;
import com.apparat.service.MaintenanceNotFoundException;
import com.apparat.service.MaintenanceService;
import com.apparat.service.ResourceNotFoundException;
import com.apparat.service.ResourceService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Maintenance windows for staff. AuthFilter already restricts
 * every /manage URL to technicians.
 */
@WebServlet("/manage/maintenance")
public class ManageMaintenanceServlet extends BaseServlet {

    private final MaintenanceService maintenanceService =
            new MaintenanceService();

    private final ResourceService resourceService =
            new ResourceService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        // Show and consume the one-time message left by a previous POST.
        HttpSession session = request.getSession(false);

        if (session != null) {
            Object message = session.getAttribute("maintenanceMessage");

            if (message != null) {
                request.setAttribute("maintenanceMessage", message);
                session.removeAttribute("maintenanceMessage");
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

        SessionUser sessionUser = currentUser(request);

        String action = request.getParameter("action");

        // Keep what was typed, so the form does not have to be refilled.
        request.setAttribute("resourceId", request.getParameter("resourceId"));
        request.setAttribute("start", request.getParameter("start"));
        request.setAttribute("end", request.getParameter("end"));
        request.setAttribute("reason", request.getParameter("reason"));

        try {
            if ("add".equals(action)) {
                maintenanceService.addWindow(
                        Integer.parseInt(request.getParameter("resourceId")),
                        LocalDateTime.parse(request.getParameter("start")),
                        LocalDateTime.parse(request.getParameter("end")),
                        request.getParameter("reason"),
                        sessionUser.id()
                );
                flash(request, "Maintenance window added.");

            } else if ("delete".equals(action)) {
                maintenanceService.deleteWindow(
                        Integer.parseInt(request.getParameter("id"))
                );
                flash(request, "Maintenance window removed.");

            } else {
                throw new IllegalArgumentException("Unknown action.");
            }

            // Redirect after a successful POST, so a refresh cannot repeat it.
            response.sendRedirect(
                    request.getContextPath() + "/manage/maintenance"
            );

        } catch (MaintenanceConflictException e) {
            fail(request, response, HttpServletResponse.SC_CONFLICT,
                    e.getMessage());

        } catch (ResourceNotFoundException | MaintenanceNotFoundException e) {
            fail(request, response, HttpServletResponse.SC_NOT_FOUND,
                    e.getMessage());

        } catch (NumberFormatException | DateTimeParseException e) {
            fail(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "Please check your inputs.");

        } catch (IllegalArgumentException e) {
            fail(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage());

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to update maintenance windows.", e
            );
        }
    }

    private void flash(HttpServletRequest request, String message) {
        request.getSession().setAttribute("maintenanceMessage", message);
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
            request.setAttribute(
                    "resources", resourceService.listResources()
            );
            request.setAttribute(
                    "windows", maintenanceService.listUpcoming()
            );

            request.getRequestDispatcher(
                    "/WEB-INF/views/manage-maintenance.jsp"
            ).forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load maintenance windows.", e
            );
        }
    }
}
