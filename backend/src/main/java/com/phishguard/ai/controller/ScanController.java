package com.phishguard.ai.controller;

import com.phishguard.ai.dto.ApiDtos.*;
import com.phishguard.ai.dto.ScanRequests.*;
import com.phishguard.ai.service.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ScanController {
    private final UrlAnalysisService urls;
    private final MessageAnalysisService messages;
    private final ScanHistoryService history;

    public ScanController(UrlAnalysisService urls, MessageAnalysisService messages, ScanHistoryService history) {
        this.urls = urls; this.messages = messages; this.history = history;
    }

    @PostMapping("/scan/url")
    public ScanResultDto scanUrl(@Valid @RequestBody UrlScanRequest req) { return urls.scan(req.url()); }

    @PostMapping("/scan/message")
    public ScanResultDto scanMessage(@Valid @RequestBody MessageScanRequest req) { return messages.scan(req.message(), req.allowExternalLookup()); }

    @GetMapping("/scans")
    public PageDto<ScanListItemDto> list(@RequestParam(required = false) String q, @RequestParam(required = false) String type,
                                         @RequestParam(required = false) String classification, @RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to, @RequestParam(required = false) String sort,
                                         @RequestParam(required = false) String dir, @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return history.list(new ScanHistoryService.Query(q, type, classification, from, to, sort, dir, page, size));
    }

    @GetMapping("/scans/{id}")
    public ScanResultDto get(@PathVariable UUID id) { return history.get(id); }

    @DeleteMapping("/scans/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { history.delete(id); return ResponseEntity.noContent().build(); }
}
