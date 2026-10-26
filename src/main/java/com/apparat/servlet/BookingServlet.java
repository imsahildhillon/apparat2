package com.apparat.servlet;

import com.apparat.dto.SessionUser;
import com.apparat.model.Resource;
import com.apparat.model.Booking;
import com.apparat.policy.BookingPolicy;
import com.apparat.service.BookingLimitException;
import com.apparat.service.BookingService;
import com.apparat.service.MaintenanceConflictException;
import com.apparat.service.ResourceService;
import com.apparat.service.SlotConflictException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/book")
public class BookingServlet extends BaseServlet {

    private final BookingService bookingService =
            new BookingService();

    private final ResourceService resourceService =
            new ResourceService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        // Keep the instrument and date chosen on another page, such as the
        // Book link on the instrument list or the availability page.
        request.setAttribute(
                "resourceId",
                request.getParameter("resourceId")
        );
        request.setAttribute("date", request.getParameter("date"));

        showForm(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // The logged-in user, put in the session by LoginServlet.
        SessionUser sessionUser = currentUser(request);
        int userId = sessionUser.id();

        String resourceIdText = request.getParameter("resourceId");
        String dateText = request.getParameter("date");
        String startTimeText = request.getParameter("startTime");
        String endTimeText = request.getParameter("endTime");

        // Preserve submitted values when validation fails.
        request.setAttribute("resourceId", resourceIdText);
        request.setAttribute("date", dateText);
        request.setAttribute("startTime", startTimeText);
        request.setAttribute("endTime", endTimeText);

        try {
            // 1. Say exactly which field is missing.
            if (isBlank(resourceIdText)) {
                throw new IllegalArgumentException(
                        "Please choose an instrument."
                );
            }
            if (isBlank(dateText)) {
                throw new IllegalArgumentException(
                        "Please choose a date."
                );
            }
            if (isBlank(startTimeText)) {
                throw new IllegalArgumentException(
                        "Please choose a start time."
                );
            }
            if (isBlank(endTimeText)) {
                throw new IllegalArgumentException(
                        "Please choose an end time."
                );
            }

            // 2. Turn the text into numbers and times.
            int resourceId = Integer.parseInt(resourceIdText);
            LocalDate date = LocalDate.parse(dateText);

            LocalDateTime start =
                    LocalDateTime.of(date, LocalTime.parse(startTimeText));
            LocalDateTime end =
                    LocalDateTime.of(date, LocalTime.parse(endTimeText));

            // 3. A booking cannot start in the past.
            if (start.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException(
                        "That time has already passed. "
                                + "Please choose a later start time."
                );
            }

            Booking booking = bookingService.createBooking(
                    userId,
                    resourceId,
                    start,
                    end
            );

            // 4. Leave a confirmation for the My bookings page, which
            //    shows it once. Then redirect, so a refresh cannot
            //    submit the booking again.
            request.getSession().setAttribute(
                    "bookingMessage",
                    "Booking #" + booking.getId() + " confirmed: "
                            + instrumentName(resourceId) + " on " + date
                            + ", " + startTimeText + " - " + endTimeText + "."
            );

            response.sendRedirect(
                    request.getContextPath() + "/bookings"
            );

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute(
                    "message",
                    "Please choose an instrument from the list."
            );
            showForm(request, response);

        } catch (DateTimeParseException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute(
                    "message",
                    "Please choose the date and times from the lists."
            );
            showForm(request, response);

        } catch (MaintenanceConflictException e) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            request.setAttribute(
                    "message",
                    e.getMessage()
                            + " Check the availability page for free times."
            );
            showForm(request, response);

        } catch (BookingLimitException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.setAttribute("message", e.getMessage());
            showForm(request, response);

        } catch (SlotConflictException e) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            request.setAttribute(
                    "message",
                    "That time is no longer free: someone else has booked "
                            + "part of it. Check the availability page and "
                            + "pick another time."
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // The instrument's name, for the confirmation message.
    private String instrumentName(int resourceId) throws SQLException {

        for (Resource resource : resourceService.listResources()) {
            if (resource.getId() == resourceId) {
                return resource.getName();
            }
        }

        return "the instrument";
    }

    // Every start time offered, such as 09:00 ... 17:30, built from the
    // same opening hours and slot length the booking rules use.
    private List<String> timeOptions(boolean forStartTimes) {

        List<String> times = new ArrayList<>();
        LocalTime time = BookingService.OPEN;

        while (!time.isAfter(BookingService.CLOSE)) {
            times.add(time.toString());
            time = time.plusMinutes(BookingService.SLOT_MINUTES);
        }

        // A booking cannot start at closing time or end at opening time.
        if (forStartTimes) {
            times.remove(times.size() - 1);
        } else {
            times.remove(0);
        }

        return times;
    }

    private void showForm(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        try {
            List<Resource> resources =
                    resourceService.listResources();

            request.setAttribute("resources", resources);

            request.setAttribute("startTimes", timeOptions(true));
            request.setAttribute("endTimes", timeOptions(false));
            request.setAttribute("today", LocalDate.now().toString());
            request.setAttribute("openTime", BookingService.OPEN.toString());
            request.setAttribute("closeTime", BookingService.CLOSE.toString());

            // What this user's role allows, shown on the page and used by the
            // browser to offer only end times within the limit.
            BookingPolicy policy =
                    BookingPolicy.forRole(currentUser(request).role());

            long openMinutes = Duration.between(
                    BookingService.OPEN, BookingService.CLOSE).toMinutes();

            request.setAttribute("policyText", policy.describe());
            request.setAttribute(
                    "maxMinutes",
                    Math.min(policy.maxMinutesPerBooking(), openMinutes)
            );

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
