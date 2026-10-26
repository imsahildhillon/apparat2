package com.apparat.servlet;

import com.apparat.dto.SessionUser;
import com.apparat.service.BookingNotFoundException;
import com.apparat.service.BookingOwnershipException;
import com.apparat.service.BookingService;
import com.apparat.service.BookingStateException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/cancel")
public class CancelBookingServlet extends BaseServlet {

    private final BookingService bookingService = new BookingService();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // The logged-in user, put in the session by LoginServlet.
        SessionUser sessionUser = currentUser(request);
        int userId = sessionUser.id();

        String rawBookingId = request.getParameter("bookingId");

        if (rawBookingId == null || rawBookingId.isBlank()) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Booking ID is required."
            );
            return;
        }

        final int bookingId;

        try {
            bookingId = Integer.parseInt(rawBookingId);
            if (bookingId <= 0) {
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid booking ID."
                );
                return;
            }
        } catch (NumberFormatException e) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid booking ID."
            );
            return;
        }

        try {
            bookingService.cancelBooking(bookingId, userId);

            request.getSession().setAttribute(
                    "bookingMessage",
                    "Booking #" + bookingId + " cancelled successfully."
            );

            response.sendRedirect(
                    request.getContextPath() + "/bookings"
            );

        } catch (BookingNotFoundException e) {
            response.sendError(
                    HttpServletResponse.SC_NOT_FOUND,
                    e.getMessage()
            );
        } catch (BookingOwnershipException e) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    e.getMessage()
            );
        } catch (BookingStateException e) {
            response.sendError(
                    HttpServletResponse.SC_CONFLICT,
                    e.getMessage()
            );
        } catch (IllegalArgumentException e) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage()
            );
        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to cancel the booking.", e
            );
        }
    }
}
