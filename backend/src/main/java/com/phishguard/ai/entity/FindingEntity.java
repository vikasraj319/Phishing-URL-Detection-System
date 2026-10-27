package com.phishguard.ai.entity;

import com.phishguard.ai.detection.Severity;
import jakarta.persistence.*;

@Entity
@Table(name = "scan_findings")
public class FindingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private ScanEntity scan;

    @Column(name = "rule_id", nullable = false, length = 64)
    private String ruleId;

    @Column(nullable = false, length = 160)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Column(nullable = false, columnDefinition = "text")
    private String explanation;

    @Column(length = 512)
    private String evidence;

    @Column(name = "score_contribution", nullable = false)
    private int scoreContribution;

    protected FindingEntity() {}

    public FindingEntity(String ruleId, String title, Severity severity, String explanation, String evidence, int contribution) {
        this.ruleId = ruleId; this.title = title; this.severity = severity; this.explanation = explanation;
        this.evidence = evidence != null && evidence.length() > 512 ? evidence.substring(0, 509) + "..." : evidence;
        this.scoreContribution = contribution;
    }

    void setScan(ScanEntity s) { this.scan = s; }
    public String getRuleId() { return ruleId; }
    public String getTitle() { return title; }
    public Severity getSeverity() { return severity; }
    public String getExplanation() { return explanation; }
    public String getEvidence() { return evidence; }
    public int getScoreContribution() { return scoreContribution; }
}
