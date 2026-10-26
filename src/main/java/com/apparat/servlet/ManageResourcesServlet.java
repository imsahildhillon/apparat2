package com.apparat.servlet;

import com.apparat.model.ResourceStatus;
import com.apparat.service.DuplicateResourceException;
import com.apparat.service.ResourceAdminService;
import com.apparat.service.ResourceNotFoundException;
import com.apparat.service.ResourceService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Instrument management for staff. AuthFilter already restricts
 * every /manage URL to technicians.
 */
@WebServlet("/manage/resources")
public class ManageResourcesServlet extends HttpServlet {

    private final ResourceService resourceService =
            new ResourceService();

    private final ResourceAdminService resourceAdminService =
            new ResourceAdminService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        // Show and consume the one-time messages left by a previous POST.
        HttpSession session = request.getSession(false);

        if (session != null) {
            for (String name : new String[]{"manageMessage", "importErrors"}) {
                Object value = session.getAttribute(name);

                if (value != null) {
                    request.setAttribute(name, value);
                    session.removeAttribute(name);
                }
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

        String action = request.getParameter("action");

        try {
            if ("add".equals(action)) {
                resourceAdminService.addResource(
                        request.getParameter("name"),
                        request.getParameter("location")
                );
                flash(request, "Instrument added.");

            } else if ("update".equals(action)) {
                resourceAdminService.updateResource(
                        Integer.parseInt(request.getParameter("id")),
                        request.getParameter("name"),
                        request.getParameter("location")
                );
                flash(request, "Instrument updated.");

            } else if ("status".equals(action)) {
                resourceAdminService.setStatus(
                        Integer.parseInt(request.getParameter("id")),
                        ResourceStatus.valueOf(
                                request.getParameter("status")
                        )
                );
                flash(request, "Instrument status changed.");

            } else {
                throw new IllegalArgumentException("Unknown action.");
            }

            // Redirect after a successful POST, so a refresh cannot repeat it.
            response.sendRedirect(
                    request.getContextPath() + "/manage/resources"
            );

        } catch (DuplicateResourceException e) {
            fail(request, response, HttpServletResponse.SC_CONFLICT,
                    e.getMessage());

        } catch (ResourceNotFoundException e) {
            fail(request, response, HttpServletResponse.SC_NOT_FOUND,
                    e.getMessage());

        } catch (NumberFormatException e) {
            fail(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "Please check your inputs.");

        } catch (IllegalArgumentException e) {
            fail(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage());

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to update instruments.", e
            );
        }
    }

    private void flash(HttpServletRequest request, String message) {
        request.getSession().setAttribute("manageMessage", message);
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

            request.getRequestDispatcher(
                    "/WEB-INF/views/manage-resources.jsp"
            ).forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load instruments.", e
            );
        }
    }
}
