package com.phishguard.ai.detection;

public record UrlAnalysisResult(NormalizedUrl url, UrlFeatures features, RiskAssessment assessment) {}
