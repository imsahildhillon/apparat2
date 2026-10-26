package com.apparat.servlet;

import com.apparat.dto.BookingView;
import com.apparat.dto.SessionUser;
import com.apparat.service.BookingService;
import com.apparat.util.CsvUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

/**
 * Downloads the logged-in user's own bookings as a CSV file.
 */
@WebServlet("/export/bookings")
public class ExportBookingsServlet extends BaseServlet {

    private final BookingService bookingService = new BookingService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        // Only the logged-in user's own bookings are ever exported.
        SessionUser sessionUser = currentUser(request);

        try {
            List<BookingView> bookings =
                    bookingService.listBookings(sessionUser.id());

            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader(
                    "Content-Disposition",
                    "attachment; filename=\"my_bookings.csv\""
            );

            PrintWriter writer = response.getWriter();

            writer.println(CsvUtil.toLine(
                    "booking_id", "instrument", "start", "end", "status"
            ));

            for (BookingView booking : bookings) {
                writer.println(CsvUtil.toLine(
                        String.valueOf(booking.id()),
                        booking.resourceName(),
                        String.valueOf(booking.startTime()),
                        String.valueOf(booking.endTime()),
                        booking.status()
                ));
            }

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to export your bookings.", e
            );
        }
    }
}
