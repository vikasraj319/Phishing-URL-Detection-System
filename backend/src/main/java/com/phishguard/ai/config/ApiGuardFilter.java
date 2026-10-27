package com.phishguard.ai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects oversized bodies (413) and applies a per-client fixed-window rate limit (429). The window state is in
 * memory and keyed by the TCP peer address; behind a reverse proxy, put the limit at the proxy instead.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiGuardFilter extends OncePerRequestFilter {
    static final long MAX_BODY_BYTES = 64 * 1024;
    private record Window(long minute, int count) {}

    private final AppProperties props;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public ApiGuardFilter(AppProperties props, Clock clock) {
        this.props = props; this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) { return !request.getRequestURI().startsWith("/api/"); }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        if ("OPTIONS".equals(req.getMethod())) { chain.doFilter(req, res); return; }
        if (req.getContentLengthLong() > MAX_BODY_BYTES) { reject(res, 413, "PAYLOAD_TOO_LARGE", "Request body is too large."); return; }

        boolean scan = "POST".equals(req.getMethod());
        int limit = scan ? props.rateLimit().scansPerMinute() : props.rateLimit().readsPerMinute();
        long minute = clock.millis() / 60_000;
        String key = (scan ? "w:" : "r:") + req.getRemoteAddr();
        Window w = windows.merge(key, new Window(minute, 1), (old, fresh) -> old.minute() == minute ? new Window(minute, old.count() + 1) : fresh);
        if (windows.size() > 10_000) evict(minute);
        if (w.count() > limit) {
            res.setHeader("Retry-After", "60");
            reject(res, 429, "RATE_LIMITED", "Too many requests. Please wait a minute and try again.");
            return;
        }
        chain.doFilter(req, res);
    }

    private void evict(long minute) {
        for (Iterator<Map.Entry<String, Window>> it = windows.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue().minute() < minute) it.remove();
        }
    }

    private void reject(HttpServletResponse res, int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        // Fixed internal strings only (no user input), so hand-written JSON is safe and independent of the Jackson version.
        res.getWriter().write("{\"status\":" + status + ",\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
