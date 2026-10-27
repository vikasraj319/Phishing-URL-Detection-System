package com.phishguard.ai.repository;

import com.phishguard.ai.entity.ScanEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScanRepository extends JpaRepository<ScanEntity, UUID>, JpaSpecificationExecutor<ScanEntity> {

    /** Rows of [Classification, count] for scans in [from, to). */
    @Query("select s.classification, count(s) from ScanEntity s where s.createdAt >= :from and s.createdAt < :to group by s.classification")
    List<Object[]> countByClassification(@Param("from") Instant from, @Param("to") Instant to);

    /** Rows of [yyyy-MM-dd (UTC day), count]. */
    @Query(value = "select cast((created_at at time zone 'UTC')::date as varchar), count(*) from scans where created_at >= :from and created_at < :to group by 1 order by 1", nativeQuery = true)
    List<Object[]> countPerDay(@Param("from") Instant from, @Param("to") Instant to);

    /** Rows of [ruleId, title, count], most frequent first. */
    @Query("select f.ruleId, max(f.title), count(f) from FindingEntity f where f.scan.createdAt >= :from and f.scan.createdAt < :to group by f.ruleId order by count(f) desc")
    List<Object[]> topIndicators(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Modifying
    @Query("delete from ScanEntity s where s.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
