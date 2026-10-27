package com.phishguard.ai.detection;

import java.util.List;

public record RiskAssessment(int score, Classification classification, List<ScoredFinding> findings) {}
