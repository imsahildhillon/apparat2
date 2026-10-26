package com.apparat.servlet;

import com.apparat.model.Resource;
import com.apparat.service.AvailabilityService;
import com.apparat.service.ResourceService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@WebServlet("/availability")
public class AvailabilityServlet extends HttpServlet {

    private final AvailabilityService availabilityService =
            new AvailabilityService();

    private final ResourceService resourceService =
            new ResourceService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String resourceIdText = request.getParameter("resourceId");
        String dateText = request.getParameter("date");

        // Default to today, so the page is useful straight away.
        LocalDate date = LocalDate.now();

        try {
            List<Resource> resources = resourceService.listResources();
            request.setAttribute("resources", resources);

            if (dateText != null && !dateText.isBlank()) {
                date = LocalDate.parse(dateText);
            }

            request.setAttribute("date", date);

            // No instrument chosen yet: show only the selector.
            if (resourceIdText != null && !resourceIdText.isBlank()) {
                int resourceId = Integer.parseInt(resourceIdText);

                Optional<Resource> selected = resources.stream()
                        .filter(resource -> resource.getId() == resourceId)
                        .findFirst();

                if (selected.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    request.setAttribute(
                            "error", "That instrument does not exist."
                    );
                } else {
                    request.setAttribute("selected", selected.get());
                    request.setAttribute(
                            "grid",
                            availabilityService.getDayGrid(resourceId, date)
                    );
                }
            }

        } catch (NumberFormatException | DateTimeParseException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute(
                    "error", "Please choose a valid instrument and date."
            );
            request.setAttribute("date", LocalDate.now());

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load availability.", e
            );
        }

        request.getRequestDispatcher(
                "/WEB-INF/views/availability.jsp"
        ).forward(request, response);
    }
}
