package com.apparat.dto;

import java.util.List;

/**
 * Outcome of a CSV import: how many rows were saved,
 * and one message per rejected row.
 */
public record ImportResult(
        int imported,
        List<String> errors
) {}
