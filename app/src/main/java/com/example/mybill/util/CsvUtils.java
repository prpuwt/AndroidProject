package com.example.mybill.util;

import java.util.ArrayList;
import java.util.List;

public final class CsvUtils {
    private CsvUtils() {}

    public static String escape(String field) {
        if (field == null) return "\"\"";
        if (field.contains("\"") || field.contains(",") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return "\"" + field + "\"";
    }

    public static String unescape(String field) {
        if (field == null) return "";
        field = field.trim();
        if (field.startsWith("\"") && field.endsWith("\"")) {
            field = field.substring(1, field.length() - 1);
            field = field.replace("\"\"", "\"");
        }
        return field;
    }

    public static String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(sb.toString().trim());
                    sb = new StringBuilder();
                } else {
                    sb.append(c);
                }
            }
        }
        fields.add(sb.toString().trim());
        return fields.toArray(new String[0]);
    }
}
