package com.phishguard.ai.util;

import com.phishguard.ai.detection.NormalizedUrl;
import java.util.regex.Pattern;

/** Builds the minimised identifier stored in history. Full messages and URL queries are never kept. */
public final class Redactor {
    private static final Pattern URL = Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
    private static final Pattern DIGITS = Pattern.compile("\\d{4,}");

    private Redactor() {}

    public static String messageSummary(String text) {
        String s = text.replaceAll("\\s+", " ").strip();
        s = URL.matcher(s).replaceAll("[link]");
        s = EMAIL.matcher(s).replaceAll("[email]");
        s = DIGITS.matcher(s).replaceAll("####");
        if (s.length() > 60) s = s.substring(0, 57).stripTrailing() + "...";
        return s;
    }

    public static String urlDisplay(NormalizedUrl u) {
        String path = "/".equals(u.path()) ? "" : u.path();
        String shown = u.host() + path + (u.query() != null ? "?..." : "");
        return shown.length() > 200 ? shown.substring(0, 197) + "..." : shown;
    }
}
