package com.phishguard.ai.service;

import com.phishguard.ai.detection.Classification;
import com.phishguard.ai.detection.RiskAssessment;
import com.phishguard.ai.detection.ScoredFinding;
import com.phishguard.ai.detection.InvalidInputException;
import com.phishguard.ai.dto.ApiDtos.*;
import com.phishguard.ai.entity.*;
import com.phishguard.ai.exception.NotFoundException;
import com.phishguard.ai.repository.ScanRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScanHistoryService {
    private static final Map<String, String> SORTABLE = Map.of("createdAt", "createdAt", "score", "riskScore", "type", "scanType", "classification", "classification");
    private final ScanRepository repo;
    private final Clock clock;

    public ScanHistoryService(ScanRepository repo, Clock clock) { this.repo = repo; this.clock = clock; }

    /** Persists the assessment and returns the API view. The returned findings keep message spans (transient). */
    @Transactional
    public ScanResultDto save(ScanType type, String display, String inputHash, RiskAssessment a,
                              List<ProviderResultDto> providers, List<String> urls, String messageText) {
        ScanEntity e = new ScanEntity(UUID.randomUUID(), type, display, inputHash, a.score(), a.classification(), providers, urls, clock.instant());
        for (ScoredFinding sf : a.findings()) {
            var f = sf.finding();
            e.addFinding(new FindingEntity(f.ruleId(), f.title(), f.severity(), f.explanation(), f.evidence(), sf.scoreContribution()));
        }
        repo.save(e);
        List<FindingDto> findings = a.findings().stream().map(sf -> new FindingDto(sf.finding().ruleId(), sf.finding().title(),
            sf.finding().severity().name(), sf.finding().explanation(), sf.finding().evidence(), sf.scoreContribution(),
            sf.finding().spans().stream().map(s -> new SpanDto(s.start(), s.end())).toList())).toList();
        return new ScanResultDto(e.getId(), type.name(), display, a.score(), a.classification().name(), findings, providers, urls, e.getCreatedAt(), messageText);
    }

    @Transactional(readOnly = true)
    public ScanResultDto get(UUID id) {
        ScanEntity e = repo.findById(id).orElseThrow(() -> new NotFoundException("Scan not found."));
        List<FindingDto> findings = e.getFindings().stream().map(f -> new FindingDto(f.getRuleId(), f.getTitle(), f.getSeverity().name(),
            f.getExplanation(), f.getEvidence(), f.getScoreContribution(), List.of())).toList();
        return new ScanResultDto(e.getId(), e.getScanType().name(), e.getInputDisplay(), e.getRiskScore(), e.getClassification().name(),
            findings, e.getProviderResults(), e.getExtractedUrls(), e.getCreatedAt(), null);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new NotFoundException("Scan not found.");
        repo.deleteById(id);
    }

    @Transactional
    public int purgeOlderThan(Instant cutoff) { return repo.deleteOlderThan(cutoff); }

    public record Query(String q, String type, String classification, String from, String to, String sort, String dir, int page, int size) {}

    @Transactional(readOnly = true)
    public PageDto<ScanListItemDto> list(Query q) {
        ScanType type = parseEnum(ScanType.class, q.type(), "type");
        Classification cls = parseEnum(Classification.class, q.classification(), "classification");
        Instant from = parseDate(q.from(), 0), to = parseDate(q.to(), 1);
        String sortField = SORTABLE.get(q.sort() == null || q.sort().isBlank() ? "createdAt" : q.sort());
        if (sortField == null) throw new InvalidInputException("Unsupported sort field.");
        Sort sort = Sort.by("asc".equalsIgnoreCase(q.dir()) ? Sort.Direction.ASC : Sort.Direction.DESC, sortField).and(Sort.by(Sort.Direction.DESC, "id"));
        int size = Math.max(1, Math.min(100, q.size() <= 0 ? 10 : q.size()));
        String search = q.q() == null ? "" : q.q().strip().toLowerCase();

        Specification<ScanEntity> spec = (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (!search.isEmpty()) ps.add(cb.like(cb.lower(root.get("inputDisplay")), "%" + escapeLike(search) + "%", '\\'));
            if (type != null) ps.add(cb.equal(root.get("scanType"), type));
            if (cls != null) ps.add(cb.equal(root.get("classification"), cls));
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) ps.add(cb.lessThan(root.get("createdAt"), to));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<ScanEntity> page = repo.findAll(spec, PageRequest.of(Math.max(0, q.page()), size, sort));
        List<ScanListItemDto> items = page.getContent().stream().map(e -> new ScanListItemDto(e.getId(), e.getScanType().name(),
            e.getInputDisplay(), e.getRiskScore(), e.getClassification().name(), e.getCreatedAt())).toList();
        return new PageDto<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    static String escapeLike(String s) { return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"); }

    private static <E extends Enum<E>> E parseEnum(Class<E> c, String v, String field) {
        if (v == null || v.isBlank()) return null;
        try { return Enum.valueOf(c, v); } catch (IllegalArgumentException e) { throw new InvalidInputException("Unsupported " + field + " filter."); }
    }

    /** yyyy-MM-dd as a UTC day boundary; plusDays=1 makes an inclusive end date an exclusive bound. */
    private static Instant parseDate(String s, int plusDays) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s).plusDays(plusDays).atStartOfDay(ZoneOffset.UTC).toInstant(); }
        catch (DateTimeParseException e) { throw new InvalidInputException("Dates must use the yyyy-MM-dd format."); }
    }
}
