package com.phishguard.ai.util;

import static org.junit.jupiter.api.Assertions.*;

import com.phishguard.ai.detection.InvalidInputException;
import com.phishguard.ai.detection.UrlNormalizer;
import java.time.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UtilTest {
    @Test void messageSummaryRedactsLinksEmailsAndLongNumbers() {
        assertEquals("Your OTP #### sent to [email] see [link] now", Redactor.messageSummary("Your OTP 482913 sent to bob@mail.com see https://x.test/a?b=1  now"));
        assertEquals(60, Redactor.messageSummary("a".repeat(100)).length());
    }
    @Test void urlDisplayDropsCredentialsAndQuery() {
        assertEquals("example.com/path/a?...", Redactor.urlDisplay(UrlNormalizer.normalize("https://user:pw@Example.com/path/a?token=secret#frag")));
    }
    @Test void windowsCoverTodayAndPreviousPeriodIsAdjacent() {
        var w = DateWindows.current("7d", Clock.fixed(Instant.parse("2026-10-24T23:59:00Z"), ZoneOffset.UTC));
        assertEquals(LocalDate.of(2026, 10, 18), w.firstDay());
        assertEquals(Instant.parse("2026-10-25T00:00:00Z"), w.to());
        assertEquals(w.from(), w.previous().to());
        var pts = DateWindows.fill(Map.of(LocalDate.of(2026, 10, 24), 3L), w);
        assertEquals(7, pts.size());
        assertEquals(0L, pts.get(0).count());
        assertEquals(3L, pts.get(6).count());
    }
    @Test void unsupportedRangeRejected() { assertThrows(InvalidInputException.class, () -> DateWindows.parseDays("90d")); }
}
