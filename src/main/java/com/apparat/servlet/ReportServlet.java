package com.apparat.servlet;

import com.apparat.dto.UtilizationRow;
import com.apparat.service.ReportService;
import com.apparat.util.CsvUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Utilization report for staff, as a page or as a CSV download
 * (/reports?format=csv). AuthFilter restricts /reports to technicians.
 */
@WebServlet("/reports")
public class ReportServlet extends HttpServlet {

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        // Default: the last 30 days, ending today.
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(29);

        String fromText = request.getParameter("from");
        String toText = request.getParameter("to");

        try {
            if (fromText != null && !fromText.isBlank()) {
                from = LocalDate.parse(fromText);
            }

            if (toText != null && !toText.isBlank()) {
                to = LocalDate.parse(toText);
            }

            List<UtilizationRow> rows = reportService.utilization(from, to);

            if ("csv".equals(request.getParameter("format"))) {
                writeCsv(response, rows, from, to);
                return;
            }

            request.setAttribute("rows", rows);

            // Totals for the footer row.
            request.setAttribute(
                    "totalBookings",
                    rows.stream().mapToInt(UtilizationRow::bookings).sum()
            );
            request.setAttribute(
                    "totalHours",
                    rows.stream()
                            .mapToDouble(UtilizationRow::bookedHours)
                            .sum()
            );

        } catch (DateTimeParseException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", "Please enter valid dates.");

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", e.getMessage());

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to build the report.", e
            );
        }

        request.setAttribute("from", from);
        request.setAttribute("to", to);

        request.getRequestDispatcher(
                "/WEB-INF/views/report.jsp"
        ).forward(request, response);
    }

    private void writeCsv(
            HttpServletResponse response,
            List<UtilizationRow> rows,
            LocalDate from,
            LocalDate to
    ) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"utilization_" + from
                        + "_to_" + to + ".csv\""
        );

        PrintWriter writer = response.getWriter();

        writer.println(CsvUtil.toLine(
                "instrument", "bookings", "booked_hours", "utilization_percent"
        ));

        for (UtilizationRow row : rows) {
            writer.println(CsvUtil.toLine(
                    row.resourceName(),
                    String.valueOf(row.bookings()),
                    String.valueOf(row.bookedHours()),
                    String.valueOf(row.utilizationPercent())
            ));
        }
    }
}
