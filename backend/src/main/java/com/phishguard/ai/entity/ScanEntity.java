package com.phishguard.ai.entity;

import com.phishguard.ai.detection.Classification;
import com.phishguard.ai.dto.ApiDtos.ProviderResultDto;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "scans")
public class ScanEntity {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_type", nullable = false, length = 16)
    private ScanType scanType;

    @Column(name = "input_display", nullable = false, length = 512)
    private String inputDisplay;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Classification classification;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection
    @CollectionTable(name = "scan_providers", joinColumns = @JoinColumn(name = "scan_id"))
    @OrderColumn(name = "idx")
    private List<ProviderResultEmbeddable> providers = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "scan_urls", joinColumns = @JoinColumn(name = "scan_id"))
    @OrderColumn(name = "idx")
    @Column(name = "url", nullable = false, length = 2048)
    private List<String> extractedUrls = new ArrayList<>();

    @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("scoreContribution DESC")
    private List<FindingEntity> findings = new ArrayList<>();

    protected ScanEntity() {}

    public ScanEntity(UUID id, ScanType type, String inputDisplay, String inputHash, int riskScore,
                      Classification classification, List<ProviderResultDto> providerResults, List<String> urls, Instant createdAt) {
        this.id = id; this.scanType = type; this.inputDisplay = inputDisplay; this.inputHash = inputHash;
        this.riskScore = riskScore; this.classification = classification; this.createdAt = createdAt;
        providerResults.forEach(p -> this.providers.add(new ProviderResultEmbeddable(p)));
        urls.forEach(u -> this.extractedUrls.add(u.length() > 2048 ? u.substring(0, 2048) : u));
    }

    public void addFinding(FindingEntity f) { f.setScan(this); findings.add(f); }

    public UUID getId() { return id; }
    public ScanType getScanType() { return scanType; }
    public String getInputDisplay() { return inputDisplay; }
    public int getRiskScore() { return riskScore; }
    public Classification getClassification() { return classification; }
    public List<ProviderResultDto> getProviderResults() { return providers.stream().map(ProviderResultEmbeddable::toDto).toList(); }
    public List<String> getExtractedUrls() { return List.copyOf(extractedUrls); }
    public Instant getCreatedAt() { return createdAt; }
    public List<FindingEntity> getFindings() { return findings; }
}
