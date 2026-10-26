package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.dao.ResourceDao;
import com.apparat.dto.ImportResult;
import com.apparat.model.Resource;
import com.apparat.model.ResourceStatus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceAdminServiceDbTests {

    private final ResourceAdminService service =
            new ResourceAdminService();

    private final ResourceDao resourceDao = new ResourceDao();

    @BeforeEach
    @AfterEach
    void cleanUp() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM resources WHERE name LIKE 'ZZ-TEST-%'")) {

            statement.executeUpdate();
        }
    }

    @Test
    void addedInstrumentIsSavedAsActive() throws SQLException {
        Resource added = service.addResource("  ZZ-TEST-Add  ", "Lab 9");

        Optional<Resource> saved = resourceDao.findById(added.getId());

        assertTrue(saved.isPresent());
        assertEquals("ZZ-TEST-Add", saved.get().getName());
        assertEquals(ResourceStatus.ACTIVE, saved.get().getStatus());
    }

    @Test
    void duplicateNameIsRejectedIgnoringCase() throws SQLException {
        service.addResource("ZZ-TEST-Dup", "Lab 1");

        assertThrows(DuplicateResourceException.class,
                () -> service.addResource("zz-test-dup", "Lab 2"));
    }

    @Test
    void blankAndTooLongValuesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addResource("   ", "Lab 1"));
        assertThrows(IllegalArgumentException.class,
                () -> service.addResource("ZZ-TEST-X", ""));
        assertThrows(IllegalArgumentException.class,
                () -> service.addResource("ZZ-TEST-" + "x".repeat(100), "Lab"));
    }

    @Test
    void updateChangesNameAndLocation() throws SQLException {
        Resource added = service.addResource("ZZ-TEST-Old", "Lab 1");

        service.updateResource(added.getId(), "ZZ-TEST-New", "Lab 2");

        Resource saved = resourceDao.findById(added.getId()).orElseThrow();

        assertEquals("ZZ-TEST-New", saved.getName());
        assertEquals("Lab 2", saved.getLocation());
    }

    @Test
    void updatingAMissingInstrumentFails() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateResource(999_999, "ZZ-TEST-Nope", "Lab"));
        assertThrows(ResourceNotFoundException.class,
                () -> service.setStatus(999_999, ResourceStatus.INACTIVE));
    }

    @Test
    void renamingToAnotherInstrumentsNameIsRejected() throws SQLException {
        service.addResource("ZZ-TEST-First", "Lab 1");
        Resource second = service.addResource("ZZ-TEST-Second", "Lab 2");

        assertThrows(DuplicateResourceException.class,
                () -> service.updateResource(
                        second.getId(), "ZZ-TEST-First", "Lab 2"));
    }

    @Test
    void keepingTheSameNameOnUpdateIsAllowed() throws SQLException {
        Resource added = service.addResource("ZZ-TEST-Same", "Lab 1");

        service.updateResource(added.getId(), "ZZ-TEST-Same", "Lab 5");

        assertEquals("Lab 5",
                resourceDao.findById(added.getId()).orElseThrow().getLocation());
    }

    @Test
    void statusCanBeSwitchedBothWays() throws SQLException {
        Resource added = service.addResource("ZZ-TEST-Status", "Lab 1");

        service.setStatus(added.getId(), ResourceStatus.INACTIVE);
        assertEquals(ResourceStatus.INACTIVE,
                resourceDao.findById(added.getId()).orElseThrow().getStatus());

        service.setStatus(added.getId(), ResourceStatus.ACTIVE);
        assertEquals(ResourceStatus.ACTIVE,
                resourceDao.findById(added.getId()).orElseThrow().getStatus());
    }

    @Test
    void importSavesGoodRowsAndReportsBadOnes()
            throws IOException, SQLException {

        String csv = String.join("\n",
                "name,location,status",              // 1 header, skipped
                "ZZ-TEST-A,Lab 1",                   // 2 ok, default ACTIVE
                "ZZ-TEST-B,Lab 2,inactive",          // 3 ok, status any case
                "ZZ-TEST-A,Lab 3",                   // 4 duplicate in file
                "ZZ-TEST-C,,ACTIVE",                 // 5 blank location
                "ZZ-TEST-D,Lab 4,BROKEN",            // 6 bad status
                "\"ZZ-TEST-E, quoted\",Lab 5",       // 7 ok, quoted comma
                "",                                  // 8 blank line, ignored
                "only-one-column");                  // 9 wrong column count

        ImportResult result = service.importFromCsv(
                new BufferedReader(new StringReader(csv)));

        assertEquals(3, result.imported());
        assertEquals(4, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("Line 4:"));
        assertTrue(result.errors().get(1).startsWith("Line 5:"));
        assertTrue(result.errors().get(2).startsWith("Line 6:"));
        assertTrue(result.errors().get(3).startsWith("Line 9:"));

        long importedNames = resourceDao.findAll().stream()
                .filter(r -> r.getName().startsWith("ZZ-TEST-"))
                .count();

        assertEquals(3, importedNames);

        Resource b = resourceDao.findAll().stream()
                .filter(r -> r.getName().equals("ZZ-TEST-B"))
                .findFirst().orElseThrow();

        assertEquals(ResourceStatus.INACTIVE, b.getStatus());
    }

    @Test
    void importSkipsNamesThatAlreadyExist()
            throws IOException, SQLException {

        service.addResource("ZZ-TEST-Existing", "Lab 1");

        ImportResult result = service.importFromCsv(new BufferedReader(
                new StringReader("ZZ-TEST-Existing,Lab 9\nZZ-TEST-Fresh,Lab 2")));

        assertEquals(1, result.imported());
        assertEquals(1, result.errors().size());
    }

    @Test
    void byteOrderMarkAtTheStartOfTheFileIsIgnored()
            throws IOException, SQLException {

        ImportResult result = service.importFromCsv(new BufferedReader(
                new StringReader("﻿name,location\nZZ-TEST-Bom,Lab 1")));

        assertEquals(1, result.imported());
        assertTrue(result.errors().isEmpty());
    }
}
