package com.phishguard.ai.service;

import com.phishguard.ai.detection.DetectionEngine;
import com.phishguard.ai.detection.RiskAssessment;
import com.phishguard.ai.detection.UrlAnalysisResult;
import com.phishguard.ai.dto.ApiDtos.ProviderResultDto;
import com.phishguard.ai.dto.ApiDtos.ScanResultDto;
import com.phishguard.ai.entity.ScanType;
import com.phishguard.ai.util.Redactor;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Pipeline: normalize -> features -> rules (DetectionEngine) -> reputation providers -> combine -> persist. Never fetches the URL. */
@Service
public class UrlAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(UrlAnalysisService.class);
    private final DetectionEngine engine;
    private final ThreatIntelligenceService intel;
    private final RiskScoringService scoring;
    private final ScanHistoryService history;

    public UrlAnalysisService(DetectionEngine engine, ThreatIntelligenceService intel, RiskScoringService scoring, ScanHistoryService history) {
        this.engine = engine; this.intel = intel; this.scoring = scoring; this.history = history;
    }

    public ScanResultDto scan(String rawUrl) {
        UrlAnalysisResult r = engine.analyzeUrl(rawUrl);
        List<ProviderResultDto> providers = intel.checkUrl(r.url().normalized());
        RiskAssessment a = scoring.combine(r.assessment(), providers);
        ScanResultDto out = history.save(ScanType.URL, Redactor.urlDisplay(r.url()), ThreatIntelligenceService.sha256(r.url().normalized()), a, providers, List.of(), null);
        log.info("URL scan {} score={} class={} providers={}", out.id(), a.score(), a.classification(), providers.size());
        return out;
    }
}
