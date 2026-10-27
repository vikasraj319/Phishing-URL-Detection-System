package com.phishguard.ai.intelligence;

import com.phishguard.ai.dto.ApiDtos.ProviderResultDto;

/** Adapter for one reputation service. Implementations must throw ProviderException on any failure, never return CLEAN. */
public interface ThreatIntelProvider {
    String name();
    boolean configured();
    ProviderResultDto check(String url) throws ProviderException;
}
