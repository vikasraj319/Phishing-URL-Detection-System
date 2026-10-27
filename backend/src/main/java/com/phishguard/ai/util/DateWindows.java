package com.phishguard.ai.util;

import com.phishguard.ai.detection.InvalidInputException;
import com.phishguard.ai.dto.ActivityPointDto;
import java.time.*;
import java.util.*;

/** UTC day windows for dashboard ranges: the last N calendar days including today. */
public final class DateWindows {
    public record Window(Instant from, Instant to, LocalDate firstDay, int days) {
        public Window previous() {
            LocalDate prevFirst = firstDay.minusDays(days);
            return new Window(prevFirst.atStartOfDay(ZoneOffset.UTC).toInstant(), from, prevFirst, days);
        }
    }

    private DateWindows() {}

    public static int parseDays(String range) {
        if ("7d".equals(range)) return 7;
        if ("30d".equals(range)) return 30;
        throw new InvalidInputException("range must be 7d or 30d.");
    }

    public static Window current(String range, Clock clock) {
        int days = parseDays(range);
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        LocalDate first = today.minusDays(days - 1L);
        return new Window(first.atStartOfDay(ZoneOffset.UTC).toInstant(), today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(), first, days);
    }

    /** One point per day in the window; days without scans are reported as zero. */
    public static List<ActivityPointDto> fill(Map<LocalDate, Long> counts, Window w) {
        List<ActivityPointDto> out = new ArrayList<>();
        for (int i = 0; i < w.days(); i++) {
            LocalDate d = w.firstDay().plusDays(i);
            out.add(new ActivityPointDto(d.toString(), counts.getOrDefault(d, 0L)));
        }
        return out;
    }
}
