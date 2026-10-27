package com.phishguard.ai.detection;

import com.phishguard.ai.util.*;
import java.time.*;
import java.util.*;

/** Dependency-free check for the framework-free helpers (run with plain java). */
public class UtilSelfCheck {
    static int bad = 0;
    static void eq(String name, Object got, Object want) {
        boolean ok = Objects.equals(got, want);
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : " got=" + got + " want=" + want));
        if (!ok) bad++;
    }
    public static void main(String[] a) {
        eq("msg redaction", Redactor.messageSummary("Your OTP 482913 sent to bob@mail.com see https://x.test/a?b=1  now"),
            "Your OTP #### sent to [email] see [link] now");
        eq("msg truncation", Redactor.messageSummary("a".repeat(100)).length(), 60);
        var u = UrlNormalizer.normalize("https://user:pw@Example.com/path/a?token=secret#frag");
        eq("url display hides creds+query", Redactor.urlDisplay(u), "example.com/path/a?...");
        Clock c = Clock.fixed(Instant.parse("2026-10-24T23:59:00Z"), ZoneOffset.UTC);
        var w = DateWindows.current("7d", c);
        eq("window first day", w.firstDay(), LocalDate.of(2026, 10, 18));
        eq("window end exclusive", w.to(), Instant.parse("2026-10-25T00:00:00Z"));
        eq("previous window ends at current start", w.previous().to(), w.from());
        var pts = DateWindows.fill(Map.of(LocalDate.of(2026, 10, 24), 3L), w);
        eq("fill length", pts.size(), 7);
        eq("fill zero day", pts.get(0).count(), 0L);
        eq("fill today", pts.get(6).count(), 3L);
        try { DateWindows.parseDays("90d"); eq("bad range rejected", false, true); } catch (InvalidInputException e) { eq("bad range rejected", true, true); }
        System.out.println(bad == 0 ? "ALL UTIL CHECKS PASSED" : "FAILURES " + bad);
        System.exit(bad == 0 ? 0 : 1);
    }
}
