
package com.apparat.servlet;

import com.apparat.model.Resource;
import com.apparat.service.ResourceService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/resources")
public class ResourceListServlet extends HttpServlet {

    private final ResourceService resourceService =
            new ResourceService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        try {
            List<Resource> resources =
                    resourceService.listResources();

            request.setAttribute("resources", resources);

            // Display the confirmation after BookingServlet redirects here.
            String bookedId = request.getParameter("booked");

            if (bookedId != null && !bookedId.isBlank()) {
                request.setAttribute("bookedId", bookedId);
            }

            request.getRequestDispatcher(
                    "/WEB-INF/views/resources.jsp"
            ).forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load resources.", e
            );
        }
    }
}

