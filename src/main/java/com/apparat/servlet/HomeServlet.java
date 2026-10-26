package com.apparat.servlet;

import com.apparat.dto.BookingView;
import com.apparat.dto.HomeBooking;
import com.apparat.dto.HomeWindow;
import com.apparat.dto.MaintenanceView;
import com.apparat.dto.SessionUser;
import com.apparat.dto.UserAccount;
import com.apparat.model.Permission;
import com.apparat.model.Resource;
import com.apparat.model.ResourceStatus;
import com.apparat.model.Role;
import com.apparat.policy.BookingPolicy;
import com.apparat.service.BookingService;
import com.apparat.service.MaintenanceService;
import com.apparat.service.ResourceService;
import com.apparat.service.UserAdminService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The home page: a short summary for the logged-in user, plus extra panels
 * for the roles that hold the matching permission.
 */
@WebServlet("/home")
public class HomeServlet extends BaseServlet {

    private static final int ITEMS_SHOWN = 3;

    private static final DateTimeFormatter DAY_AND_TIME =
            DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_ONLY =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private final BookingService bookingService = new BookingService();
    private final ResourceService resourceService = new ResourceService();
    private final MaintenanceService maintenanceService =
            new MaintenanceService();
    private final UserAdminService userAdminService = new UserAdminService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        SessionUser user = currentUser(request);
        BookingPolicy policy = BookingPolicy.forRole(user.role());
        LocalDateTime now = LocalDateTime.now();

        try {
            // 1. The user's own bookings that are confirmed and not over yet.
            List<BookingView> upcoming = bookingService
                    .listBookings(user.id()).stream()
                    .filter(b -> "CONFIRMED".equals(b.status())
                            && b.endTime().isAfter(now))
                    .sorted(Comparator.comparing(BookingView::startTime))
                    .toList();

            List<HomeBooking> shown = upcoming.stream()
                    .limit(ITEMS_SHOWN)
                    .map(b -> new HomeBooking(
                            b.id(), b.resourceName(),
                            describeTime(b.startTime(), b.endTime())))
                    .toList();

            request.setAttribute("greeting", greetingFor(now.toLocalTime()));
            request.setAttribute("policyText", policy.describe());
            request.setAttribute("activeBookings", upcoming.size());
            request.setAttribute("bookingLimit", policy.isUnlimited()
                    ? "no limit"
                    : "of " + policy.maxActiveBookings());
            request.setAttribute("upcomingBookings", shown);
            request.setAttribute("nextBooking",
                    shown.isEmpty() ? null : shown.get(0));

            // 2. How many instruments can be booked right now.
            List<Resource> resources = resourceService.listResources();

            request.setAttribute("instrumentsTotal", resources.size());
            request.setAttribute("instrumentsActive", resources.stream()
                    .filter(r -> r.getStatus() == ResourceStatus.ACTIVE)
                    .count());

            // 3. Staff: the next maintenance windows.
            if (user.role().can(Permission.MANAGE_MAINTENANCE)) {
                List<MaintenanceView> windows =
                        maintenanceService.listUpcoming();

                request.setAttribute("windows", windows.stream()
                        .limit(ITEMS_SHOWN)
                        .map(w -> new HomeWindow(
                                w.resourceName(),
                                describeTime(w.startTime(), w.endTime()),
                                w.reason()))
                        .toList());
                request.setAttribute("windowCount", windows.size());
            }

            // 4. Administrators: how many accounts there are, by role.
            if (user.role().can(Permission.MANAGE_USERS)) {
                List<UserAccount> users = userAdminService.listUsers();

                Map<String, Long> byRole = new LinkedHashMap<>();

                for (Role role : Role.values()) {
                    byRole.put(role.getLabel(), users.stream()
                            .filter(u -> u.active() && u.role() == role)
                            .count());
                }

                request.setAttribute("usersByRole", byRole);
                request.setAttribute("inactiveUsers", users.stream()
                        .filter(u -> !u.active()).count());
            }

            request.getRequestDispatcher(
                    "/WEB-INF/views/home.jsp"
            ).forward(request, response);

        } catch (SQLException e) {
            throw new ServletException("Unable to load the home page.", e);
        }
    }

    // "Good morning", "Good afternoon" or "Good evening".
    static String greetingFor(LocalTime time) {

        if (time.isBefore(LocalTime.NOON)) {
            return "Good morning";
        }

        if (time.isBefore(LocalTime.of(17, 0))) {
            return "Good afternoon";
        }

        return "Good evening";
    }

    // "Tue 27 Oct, 10:00 - 11:30"
    static String describeTime(LocalDateTime start, LocalDateTime end) {
        return start.format(DAY_AND_TIME) + " - " + end.format(TIME_ONLY);
    }
}
