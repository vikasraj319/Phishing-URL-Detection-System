package com.phishguard.ai.service;

import com.phishguard.ai.detection.Classification;
import com.phishguard.ai.dto.ActivityPointDto;
import com.phishguard.ai.dto.ApiDtos.*;
import com.phishguard.ai.repository.ScanRepository;
import com.phishguard.ai.util.DateWindows;
import com.phishguard.ai.util.DateWindows.Window;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * All figures are computed from stored scans over UTC calendar-day windows. "flaggedRate" is
 * (suspicious + high risk) / total scans: the share of scans that raised a flag. It is not an accuracy metric.
 */
@Service
@Transactional(readOnly = true)
public class DashboardAnalyticsService {
    private final ScanRepository repo;
    private final Clock clock;

    public DashboardAnalyticsService(ScanRepository repo, Clock clock) { this.repo = repo; this.clock = clock; }

    private Map<Classification, Long> counts(Window w) {
        Map<Classification, Long> m = new EnumMap<>(Classification.class);
        for (Classification c : Classification.values()) m.put(c, 0L);
        for (Object[] row : repo.countByClassification(w.from(), w.to())) m.put((Classification) row[0], ((Number) row[1]).longValue());
        return m;
    }

    public DashboardStatsDto stats(String range) {
        Window w = DateWindows.current(range, clock);
        Map<Classification, Long> c = counts(w), p = counts(w.previous());
        long total = c.values().stream().mapToLong(Long::longValue).sum();
        long prevTotal = p.values().stream().mapToLong(Long::longValue).sum();
        long high = c.get(Classification.HIGH_RISK), susp = c.get(Classification.SUSPICIOUS);
        Double rate = total == 0 ? null : (high + susp) / (double) total;
        PeriodCountsDto prev = prevTotal == 0 ? null : new PeriodCountsDto(prevTotal, p.get(Classification.HIGH_RISK), p.get(Classification.SUSPICIOUS));
        return new DashboardStatsDto(total, high, susp, c.get(Classification.SAFE_LOOKING), rate, prev);
    }

    public RiskDistributionDto distribution(String range) {
        Map<Classification, Long> c = counts(DateWindows.current(range, clock));
        long s = c.get(Classification.SAFE_LOOKING), u = c.get(Classification.SUSPICIOUS), h = c.get(Classification.HIGH_RISK);
        return new RiskDistributionDto(s + u + h, s, u, h);
    }

    public List<ActivityPointDto> activity(String range) {
        Window w = DateWindows.current(range, clock);
        Map<LocalDate, Long> m = new HashMap<>();
        for (Object[] row : repo.countPerDay(w.from(), w.to())) m.put(LocalDate.parse(String.valueOf(row[0])), ((Number) row[1]).longValue());
        return DateWindows.fill(m, w);
    }

    public List<IndicatorCountDto> indicators(String range) {
        Window w = DateWindows.current(range, clock);
        return repo.topIndicators(w.from(), w.to(), PageRequest.of(0, 8)).stream()
            .map(r -> new IndicatorCountDto((String) r[0], (String) r[1], ((Number) r[2]).longValue())).toList();
    }
}
