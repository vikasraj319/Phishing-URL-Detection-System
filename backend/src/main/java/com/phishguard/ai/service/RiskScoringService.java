package com.phishguard.ai.service;

import com.phishguard.ai.detection.*;
import com.phishguard.ai.dto.ApiDtos.ProviderResultDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Combines local findings with reputation results. A provider FLAGGED verdict is strong evidence and is added as a
 * heavy finding. CLEAN, UNKNOWN and UNAVAILABLE never lower a score.
 */
@Service
public class RiskScoringService {
    static final int INTEL_FLAG_WEIGHT = 90;
    private final RiskScorer scorer;

    public RiskScoringService(Thresholds thresholds) { this.scorer = new RiskScorer(thresholds); }

    public RiskAssessment combine(RiskAssessment local, List<ProviderResultDto> providers) {
        List<Finding> all = new ArrayList<>(local.findings().stream().map(ScoredFinding::finding).toList());
        for (ProviderResultDto p : providers) {
            if ("FLAGGED".equals(p.status())) {
                all.add(Finding.of("threat_intel_flagged", "Flagged by " + p.provider(), Severity.CRITICAL,
                    "A reputation service lists this address as malicious or unwanted.", p.detail(), INTEL_FLAG_WEIGHT));
            }
        }
        return all.size() == local.findings().size() ? local : scorer.assess(all);
    }
}
