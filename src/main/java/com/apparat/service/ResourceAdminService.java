package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.dao.ResourceAdminDao;
import com.apparat.dao.ResourceDao;
import com.apparat.dto.ImportResult;
import com.apparat.model.Resource;
import com.apparat.model.ResourceStatus;
import com.apparat.util.CsvUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class ResourceAdminService {

    public static final int MAX_TEXT_LENGTH = 100;
    public static final int MAX_IMPORT_ROWS = 500;

    private final ResourceDao resourceDao = new ResourceDao();
    private final ResourceAdminDao resourceAdminDao = new ResourceAdminDao();

    public Resource addResource(String name, String location)
            throws SQLException {

        // 1. Validate and tidy the input.
        String cleanName = clean(name);
        String cleanLocation = clean(location);
        validate(cleanName, cleanLocation);

        // 2. An instrument name must not be used twice.
        if (existingNames(-1).contains(key(cleanName))) {
            throw new DuplicateResourceException(
                    "An instrument with this name already exists."
            );
        }

        // 3. Save it. New instruments start ACTIVE.
        try (Connection connection = DatabaseConnection.getConnection()) {
            int id = resourceAdminDao.insert(
                    connection, cleanName, cleanLocation,
                    ResourceStatus.ACTIVE
            );

            return new Resource(
                    id, cleanName, cleanLocation, ResourceStatus.ACTIVE
            );
        }
    }

    public void updateResource(int id, String name, String location)
            throws SQLException {

        String cleanName = clean(name);
        String cleanLocation = clean(location);
        validate(cleanName, cleanLocation);

        requireExisting(id);

        if (existingNames(id).contains(key(cleanName))) {
            throw new DuplicateResourceException(
                    "An instrument with this name already exists."
            );
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            resourceAdminDao.update(
                    connection, id, cleanName, cleanLocation
            );
        }
    }

    public void setStatus(int id, ResourceStatus status)
            throws SQLException {

        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        requireExisting(id);

        try (Connection connection = DatabaseConnection.getConnection()) {
            resourceAdminDao.updateStatus(connection, id, status);
        }
    }

    /**
     * Reads instruments from CSV text: name,location[,status].
     * Good rows are saved together in one transaction; bad rows are
     * reported by line number and skipped.
     */
    public ImportResult importFromCsv(BufferedReader reader)
            throws IOException, SQLException {

        List<String> errors = new ArrayList<>();
        List<String[]> validRows = new ArrayList<>();

        // Names already used, so duplicates are caught too,
        // including duplicates inside the file itself.
        Set<String> knownNames = existingNames(-1);

        int lineNumber = 0;
        int dataRows = 0;
        String line;

        while ((line = reader.readLine()) != null) {
            lineNumber++;

            // Excel can put an invisible marker at the start of the file.
            if (lineNumber == 1 && line.startsWith("﻿")) {
                line = line.substring(1);
            }

            if (line.isBlank()) {
                continue;
            }

            if (lineNumber == 1 && isHeader(line)) {
                continue;
            }

            dataRows++;

            if (dataRows > MAX_IMPORT_ROWS) {
                errors.add("Stopped after " + MAX_IMPORT_ROWS + " rows.");
                break;
            }

            try {
                String[] row = parseRow(line);

                if (!knownNames.add(key(row[0]))) {
                    throw new IllegalArgumentException(
                            "\"" + row[0] + "\" already exists."
                    );
                }

                validRows.add(row);

            } catch (IllegalArgumentException e) {
                errors.add("Line " + lineNumber + ": " + e.getMessage());
            }
        }

        // Save every valid row together: all of them, or none.
        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                for (String[] row : validRows) {
                    resourceAdminDao.insert(
                            connection,
                            row[0],
                            row[1],
                            ResourceStatus.valueOf(row[2])
                    );
                }

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }

                throw e;
            }
        }

        return new ImportResult(validRows.size(), errors);
    }

    private String[] parseRow(String line) {

        List<String> fields = CsvUtil.parseLine(line);

        if (fields.size() < 2 || fields.size() > 3) {
            throw new IllegalArgumentException(
                    "Expected name,location[,status]."
            );
        }

        String name = clean(fields.get(0));
        String location = clean(fields.get(1));
        validate(name, location);

        ResourceStatus status = ResourceStatus.ACTIVE;

        if (fields.size() == 3 && !fields.get(2).isBlank()) {
            try {
                status = ResourceStatus.valueOf(
                        fields.get(2).trim().toUpperCase(Locale.ROOT)
                );
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Status must be ACTIVE or INACTIVE."
                );
            }
        }

        return new String[]{name, location, status.name()};
    }

    private boolean isHeader(String line) {
        return line.trim().toLowerCase(Locale.ROOT).startsWith("name,");
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private void validate(String name, String location) {

        // The model constructor already rejects blank values.
        new Resource(0, name, location, ResourceStatus.ACTIVE);

        if (name.length() > MAX_TEXT_LENGTH
                || location.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                    "Name and location must be at most "
                            + MAX_TEXT_LENGTH + " characters."
            );
        }
    }

    private void requireExisting(int id) throws SQLException {

        Optional<Resource> resource = resourceDao.findById(id);

        if (resource.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Instrument does not exist."
            );
        }
    }

    // Lower-cased names of all instruments except the one with ignoredId.
    private Set<String> existingNames(int ignoredId) throws SQLException {

        Set<String> names = new HashSet<>();

        for (Resource resource : resourceDao.findAll()) {
            if (resource.getId() != ignoredId) {
                names.add(key(resource.getName()));
            }
        }

        return names;
    }
}
