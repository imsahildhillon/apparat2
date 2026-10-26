package com.apparat.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvUtilTests {

    @Test
    void plainValuesAreWrittenAsIs() {
        assertEquals("SEM,Lab 1,ACTIVE",
                CsvUtil.toLine("SEM", "Lab 1", "ACTIVE"));
    }

    @Test
    void valuesWithCommasAndQuotesAreQuoted() {
        assertEquals("\"Room 2, Block A\"", CsvUtil.escape("Room 2, Block A"));
        assertEquals("\"say \"\"hi\"\"\"", CsvUtil.escape("say \"hi\""));
    }

    @Test
    void nullBecomesEmpty() {
        assertEquals("", CsvUtil.escape(null));
    }

    @Test
    void formulaLookingValuesAreNeutralised() {
        assertEquals("'=SUM(A1:A9)", CsvUtil.escape("=SUM(A1:A9)"));
        assertEquals("'@cmd", CsvUtil.escape("@cmd"));
    }

    @Test
    void simpleLineIsSplitIntoFields() {
        assertEquals(List.of("SEM", "Lab 1", "ACTIVE"),
                CsvUtil.parseLine("SEM,Lab 1,ACTIVE"));
    }

    @Test
    void quotedFieldMayContainCommasAndDoubledQuotes() {
        assertEquals(List.of("Room 2, Block A", "say \"hi\""),
                CsvUtil.parseLine("\"Room 2, Block A\",\"say \"\"hi\"\"\""));
    }

    @Test
    void emptyFieldsAreKept() {
        assertEquals(List.of("A", "", "C"), CsvUtil.parseLine("A,,C"));
        assertEquals(List.of("A", ""), CsvUtil.parseLine("A,"));
    }

    @Test
    void missingClosingQuoteIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CsvUtil.parseLine("\"unfinished,Lab")
        );
    }

    @Test
    void writingThenParsingGivesTheOriginalValues() {
        String line = CsvUtil.toLine("A, B", "he said \"x\"", "plain");

        assertEquals(List.of("A, B", "he said \"x\"", "plain"),
                CsvUtil.parseLine(line));
    }
}
