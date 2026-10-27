package com.phishguard.ai.detection;

import java.util.List;

public record MessageAnalysisResult(RiskAssessment assessment, List<String> extractedUrls,
                                    List<UrlAnalysisResult> urlResults, int characterCount) {}
