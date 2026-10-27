CREATE TABLE scans (
    id               UUID PRIMARY KEY,
    scan_type        VARCHAR(16)  NOT NULL CHECK (scan_type IN ('URL', 'MESSAGE')),
    input_display    VARCHAR(512) NOT NULL,          -- redacted/minimised identifier, never the full private message
    input_hash       VARCHAR(64)  NOT NULL,          -- SHA-256 hex of the normalised input; the input itself is not stored
    risk_score       INTEGER      NOT NULL CHECK (risk_score BETWEEN 0 AND 100),
    classification   VARCHAR(16)  NOT NULL CHECK (classification IN ('SAFE_LOOKING', 'SUSPICIOUS', 'HIGH_RISK')),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_scans_created_at ON scans (created_at DESC);
CREATE INDEX idx_scans_classification ON scans (classification, created_at DESC);
CREATE INDEX idx_scans_type ON scans (scan_type, created_at DESC);

CREATE TABLE scan_findings (
    id                 BIGSERIAL PRIMARY KEY,
    scan_id            UUID         NOT NULL REFERENCES scans (id) ON DELETE CASCADE,
    rule_id            VARCHAR(64)  NOT NULL,
    title              VARCHAR(160) NOT NULL,
    severity           VARCHAR(16)  NOT NULL,
    explanation        TEXT         NOT NULL,
    evidence           VARCHAR(512),
    score_contribution INTEGER      NOT NULL
);
CREATE INDEX idx_findings_scan ON scan_findings (scan_id);
CREATE INDEX idx_findings_rule ON scan_findings (rule_id);

-- Reputation-provider outcomes per scan (plain tables instead of JSON columns: no JSON mapper needed).
CREATE TABLE scan_providers (
    scan_id  UUID         NOT NULL REFERENCES scans (id) ON DELETE CASCADE,
    idx      INTEGER      NOT NULL,
    provider VARCHAR(64)  NOT NULL,
    status   VARCHAR(16)  NOT NULL,
    detail   VARCHAR(512),
    PRIMARY KEY (scan_id, idx)
);

-- Links found in a scanned message (never the message text).
CREATE TABLE scan_urls (
    scan_id UUID          NOT NULL REFERENCES scans (id) ON DELETE CASCADE,
    idx     INTEGER       NOT NULL,
    url     VARCHAR(2048) NOT NULL,
    PRIMARY KEY (scan_id, idx)
);
