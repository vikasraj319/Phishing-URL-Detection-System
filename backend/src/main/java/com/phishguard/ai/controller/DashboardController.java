package com.phishguard.ai.controller;

import com.phishguard.ai.dto.ActivityPointDto;
import com.phishguard.ai.dto.ApiDtos.*;
import com.phishguard.ai.service.DashboardAnalyticsService;
import com.phishguard.ai.service.ThreatIntelligenceService;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import com.phishguard.ai.repository.ScanRepository;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {
    private final DashboardAnalyticsService analytics;
    private final ThreatIntelligenceService intel;
    private final ScanRepository repo;

    public DashboardController(DashboardAnalyticsService analytics, ThreatIntelligenceService intel, ScanRepository repo) {
        this.analytics = analytics; this.intel = intel; this.repo = repo;
    }

    @GetMapping("/dashboard/stats")
    public DashboardStatsDto stats(@RequestParam(defaultValue = "7d") String range) { return analytics.stats(range); }

    @GetMapping("/dashboard/risk-distribution")
    public RiskDistributionDto distribution(@RequestParam(defaultValue = "7d") String range) { return analytics.distribution(range); }

    @GetMapping("/dashboard/scan-activity")
    public List<ActivityPointDto> activity(@RequestParam(defaultValue = "7d") String range) { return analytics.activity(range); }

    @GetMapping("/dashboard/top-indicators")
    public List<IndicatorCountDto> indicators(@RequestParam(defaultValue = "7d") String range) { return analytics.indicators(range); }

    @GetMapping("/threat-intelligence/status")
    public ThreatIntelStatusDto threatIntel() { return intel.status(); }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            repo.existsById(UUID.randomUUID()); // cheap round trip proves the database answers
            return ResponseEntity.ok(Map.of("status", "UP"));
        } catch (DataAccessException e) {
            return ResponseEntity.status(503).body(Map.of("status", "DOWN"));
        }
    }
}
