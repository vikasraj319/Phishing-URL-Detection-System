package com.phishguard.ai.detection;

/** Score cut-offs. Defaults are uncalibrated starting points and must stay configurable. */
public record Thresholds(int suspiciousMin, int highRiskMin) {
    public static Thresholds defaults() { return new Thresholds(30, 60); }

    public Thresholds {
        if (suspiciousMin < 1 || highRiskMin <= suspiciousMin || highRiskMin > 100) {
            throw new IllegalArgumentException("Require 1 <= suspiciousMin < highRiskMin <= 100");
        }
    }

    public Classification classify(int score) {
        if (score >= highRiskMin) return Classification.HIGH_RISK;
        if (score >= suspiciousMin) return Classification.SUSPICIOUS;
        return Classification.SAFE_LOOKING;
    }
}
