package com.apparat.servlet;

import com.apparat.service.BookingService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/bookings")
public class MyBookingsServlet extends HttpServlet {

    private final BookingService bookingService = new BookingService();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        // TODO: Replace with the authenticated user's ID from the session.
        int userId = 1;

        // Retrieve and consume the one-time flash message.
        HttpSession session = request.getSession(false);

        if (session != null) {
            Object message = session.getAttribute("bookingMessage");

            if (message != null) {
                request.setAttribute("bookingMessage", message);
                session.removeAttribute("bookingMessage");
            }
        }

        try {
            request.setAttribute(
                    "bookings",
                    bookingService.listBookings(userId)
            );

            request.getRequestDispatcher(
                    "/WEB-INF/views/my-bookings.jsp"
            ).forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load your bookings.", e
            );
        }
    }
}

