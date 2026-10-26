package com.apparat.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal CSV helpers: quoting on the way out, quote-aware parsing on the way in.
 */
public class CsvUtil {

    private CsvUtil() {
    }

    /**
     * Builds one CSV line from the given fields.
     */
    public static String toLine(String... fields) {

        StringBuilder line = new StringBuilder();

        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(escape(fields[i]));
        }

        return line.toString();
    }

    /**
     * Makes one value safe to place in a CSV file.
     */
    public static String escape(String value) {

        if (value == null) {
            return "";
        }

        String safe = value;

        // A cell starting with = + - or @ can run as a formula when the
        // file is opened in a spreadsheet, so neutralise it with a quote.
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }

        boolean needsQuotes = safe.contains(",")
                || safe.contains("\"")
                || safe.contains("\n")
                || safe.contains("\r");

        if (needsQuotes) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }

        return safe;
    }

    /**
     * Splits one CSV line into fields. Quoted fields may contain commas,
     * and a doubled quote inside quotes means one quote character.
     */
    public static List<String> parseLine(String line) {

        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (inQuotes) {
                if (ch == '"') {
                    boolean doubledQuote = i + 1 < line.length()
                            && line.charAt(i + 1) == '"';

                    if (doubledQuote) {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        if (inQuotes) {
            throw new IllegalArgumentException(
                    "A quoted value is missing its closing quote."
            );
        }

        fields.add(current.toString());

        return fields;
    }
}
