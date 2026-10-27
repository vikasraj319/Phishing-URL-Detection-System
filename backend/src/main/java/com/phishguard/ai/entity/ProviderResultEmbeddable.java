package com.phishguard.ai.entity;

import com.phishguard.ai.dto.ApiDtos.ProviderResultDto;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ProviderResultEmbeddable {
    @Column(name = "provider", nullable = false, length = 64)
    private String provider;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "detail", length = 512)
    private String detail;

    protected ProviderResultEmbeddable() {}

    public ProviderResultEmbeddable(ProviderResultDto d) {
        this.provider = d.provider();
        this.status = d.status();
        this.detail = d.detail() != null && d.detail().length() > 512 ? d.detail().substring(0, 509) + "..." : d.detail();
    }

    public ProviderResultDto toDto() { return new ProviderResultDto(provider, status, detail); }
}
