package com.phishguard.ai.service;

import com.phishguard.ai.detection.DetectionEngine;
import com.phishguard.ai.detection.MessageAnalysisResult;
import com.phishguard.ai.detection.RiskAssessment;
import com.phishguard.ai.dto.ApiDtos.ProviderResultDto;
import com.phishguard.ai.dto.ApiDtos.ScanResultDto;
import com.phishguard.ai.entity.ScanType;
import com.phishguard.ai.util.Redactor;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The message body is analysed locally and never leaves the server. Only links found in it are sent to reputation
 * providers, and only when the caller explicitly consents. Stored: a redacted summary, the links and the findings
 * (finding evidence quotes short matched phrases, at most 120 characters each).
 */
@Service
public class MessageAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(MessageAnalysisService.class);
    private final DetectionEngine engine;
    private final ThreatIntelligenceService intel;
    private final RiskScoringService scoring;
    private final ScanHistoryService history;

    public MessageAnalysisService(DetectionEngine engine, ThreatIntelligenceService intel, RiskScoringService scoring, ScanHistoryService history) {
        this.engine = engine; this.intel = intel; this.scoring = scoring; this.history = history;
    }

    public ScanResultDto scan(String text, boolean allowExternalLookup) {
        MessageAnalysisResult r = engine.analyzeMessage(text);
        List<ProviderResultDto> providers = allowExternalLookup && !r.extractedUrls().isEmpty()
            ? intel.checkUrls(r.urlResults().stream().map(u -> u.url().normalized()).toList()) : List.of();
        RiskAssessment a = scoring.combine(r.assessment(), providers);
        ScanResultDto out = history.save(ScanType.MESSAGE, Redactor.messageSummary(text), ThreatIntelligenceService.sha256(text), a, providers, r.extractedUrls(), text);
        log.info("Message scan {} chars={} score={} class={}", out.id(), r.characterCount(), a.score(), a.classification());
        return out;
    }
}
