package com.phishguard.ai.service;

import com.phishguard.ai.config.AppProperties;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Deletes scans older than phishguard.retention-days (0 disables automatic deletion). */
@Component
public class RetentionJob {
    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);
    private final ScanHistoryService history;
    private final AppProperties props;
    private final Clock clock;

    public RetentionJob(ScanHistoryService history, AppProperties props, Clock clock) { this.history = history; this.props = props; this.clock = clock; }

    @Scheduled(cron = "0 15 3 * * *", zone = "UTC")
    public void purge() {
        if (props.retentionDays() <= 0) return;
        int n = history.purgeOlderThan(clock.instant().minus(props.retentionDays(), ChronoUnit.DAYS));
        if (n > 0) log.info("Retention purge removed {} scans older than {} days", n, props.retentionDays());
    }
}
