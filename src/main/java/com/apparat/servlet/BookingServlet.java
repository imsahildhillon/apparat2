
package com.apparat.servlet;

import com.apparat.model.Booking;
import com.apparat.model.Resource;
import com.apparat.service.BookingService;
import com.apparat.service.ResourceService;
import com.apparat.service.SlotConflictException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@WebServlet("/book")
public class BookingServlet extends HttpServlet {

    private final BookingService bookingService =
            new BookingService();

    private final ResourceService resourceService =
            new ResourceService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        // Preserve the instrument selected through the Book link.
        request.setAttribute(
                "resourceId",
                request.getParameter("resourceId")
        );

        showForm(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // TODO: replace with session user when login is implemented.
        int userId = 1;

        String resourceIdText = request.getParameter("resourceId");
        String startText = request.getParameter("start");
        String endText = request.getParameter("end");

        // Preserve submitted values when validation fails.
        request.setAttribute("resourceId", resourceIdText);
        request.setAttribute("start", startText);
        request.setAttribute("end", endText);

        try {
            if (resourceIdText == null || resourceIdText.isBlank()
                    || startText == null || startText.isBlank()
                    || endText == null || endText.isBlank()) {
                throw new IllegalArgumentException(
                        "Please fill in all fields."
                );
            }

            int resourceId = Integer.parseInt(resourceIdText);
            LocalDateTime start = LocalDateTime.parse(startText);
            LocalDateTime end = LocalDateTime.parse(endText);

            Booking booking = bookingService.createBooking(
                    userId,
                    resourceId,
                    start,
                    end
            );

            // POST-redirect-GET: avoid resubmitting the booking.
            response.sendRedirect(
                    request.getContextPath()
                            + "/resources?booked="
                            + booking.getId()
            );

        } catch (NumberFormatException | DateTimeParseException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute(
                    "message",
                    "Please check your inputs."
            );
            showForm(request, response);

        } catch (SlotConflictException e) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            request.setAttribute(
                    "message",
                    "Someone just booked that slot. Pick another time."
            );
            showForm(request, response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("message", e.getMessage());
            showForm(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Something went wrong, try again.",
                    e
            );
        }
    }

    private void showForm(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        try {
            List<Resource> resources =
                    resourceService.listResources();

            request.setAttribute("resources", resources);

            // Convert the submitted ID into a safe numeric value
            // for comparisons with resource.id in the JSP.
            String resourceIdText =
                    (String) request.getAttribute("resourceId");

            Integer selectedResourceId = null;

            if (resourceIdText != null) {
                try {
                    selectedResourceId =
                            Integer.valueOf(resourceIdText);
                } catch (NumberFormatException ignored) {
                    // Invalid IDs must not crash JSP rendering.
                }
            }

            request.setAttribute(
                    "selectedResourceId",
                    selectedResourceId
            );

            request.getRequestDispatcher(
                    "/WEB-INF/views/booking-form.jsp"
            ).forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load the booking form.",
                    e
            );
        }
    }
}

