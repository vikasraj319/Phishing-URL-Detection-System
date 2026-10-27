package com.phishguard.ai.config;

import com.phishguard.ai.detection.DetectionEngine;
import com.phishguard.ai.detection.RuleWeights;
import com.phishguard.ai.detection.Thresholds;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DetectionConfig {
    @Bean
    Thresholds thresholds(AppProperties props) {
        return new Thresholds(props.detection().suspiciousMin(), props.detection().highRiskMin());
    }

    @Bean
    DetectionEngine detectionEngine(AppProperties props, Thresholds thresholds) {
        return new DetectionEngine(new RuleWeights(props.detection().ruleWeights()), thresholds);
    }

    @Bean
    Clock clock() { return Clock.systemUTC(); }
}
