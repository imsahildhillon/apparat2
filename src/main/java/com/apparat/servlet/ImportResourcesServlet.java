package com.apparat.servlet;

import com.apparat.dto.ImportResult;
import com.apparat.service.ResourceAdminService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

@WebServlet("/manage/import")
@MultipartConfig(maxFileSize = 1_048_576, maxRequestSize = 2_097_152)
public class ImportResourcesServlet extends HttpServlet {

    private static final int MAX_ERRORS_SHOWN = 20;

    private final ResourceAdminService resourceAdminService =
            new ResourceAdminService();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        try {
            Part file = request.getPart("file");

            // 1. A file must be present, non-empty, and named *.csv.
            if (file == null || file.getSize() == 0) {
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Please choose a CSV file."
                );
                return;
            }

            String fileName = file.getSubmittedFileName();

            if (fileName == null
                    || !fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Only .csv files are accepted."
                );
                return;
            }

            // 2. Read the file as UTF-8 text, line by line.
            ImportResult result;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            file.getInputStream(), StandardCharsets.UTF_8))) {

                result = resourceAdminService.importFromCsv(reader);
            }

            // 3. Leave a summary for the management page, then redirect.
            HttpSession session = request.getSession();

            List<String> errors = result.errors();

            session.setAttribute(
                    "manageMessage",
                    "Imported " + result.imported() + " instrument(s); "
                            + errors.size() + " row(s) rejected."
            );

            if (!errors.isEmpty()) {
                session.setAttribute(
                        "importErrors",
                        errors.subList(
                                0, Math.min(errors.size(), MAX_ERRORS_SHOWN)
                        )
                );
            }

            response.sendRedirect(
                    request.getContextPath() + "/manage/resources"
            );

        } catch (IllegalStateException e) {
            // Thrown when the upload is larger than the configured limit.
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "The file is too large (limit 1 MB)."
            );

        } catch (SQLException e) {
            throw new ServletException("Unable to import instruments.", e);
        }
    }
}
