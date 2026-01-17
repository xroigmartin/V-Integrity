-- V1__init_ledger.sql
-- Initial schema for V-Integrity Ledger (PostgreSQL)

-- 0. SCHEMA SETUP
-- Create a dedicated schema for isolation and security.
CREATE SCHEMA IF NOT EXISTS ledger;

-- Set the search path so subsequent commands apply to this schema.
SET search_path TO ledger;

-- 1. BLOCKS TABLE
-- Stores the immutable chain of blocks.
CREATE TABLE blocks (
    height BIGINT PRIMARY KEY,
    timestamp TIMESTAMPTZ NOT NULL,
    previous_hash CHAR(64) NOT NULL,
    hash CHAR(64) NOT NULL UNIQUE,
    proposer_node_id VARCHAR(100) NOT NULL,
    signature TEXT NOT NULL
);

-- 2. EVIDENCES TABLE
-- Stores the raw evidence data.
-- Uses Surrogate Key (BIGINT) for internal efficiency, UUID for public identity.
CREATE TABLE evidences (
    id BIGSERIAL PRIMARY KEY, -- Internal Surrogate Key
    evidence_id UUID NOT NULL UNIQUE, -- Public Business Key
    hash CHAR(64) NOT NULL UNIQUE,
    hash_algorithm VARCHAR(20) NOT NULL,
    homologation_id VARCHAR(80) NOT NULL,
    test_run_id VARCHAR(80) NOT NULL,
    artifact_name VARCHAR(255) NOT NULL,
    artifact_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT,
    created_by VARCHAR(120) NOT NULL,
    storage_uri TEXT,
    standards JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);

-- 3. BLOCK_EVIDENCES TABLE
-- Join table to link evidences to specific blocks.
-- Links to internal ID (BIGINT) of evidences for performance.
CREATE TABLE block_evidences (
    block_height BIGINT NOT NULL,
    evidence_internal_id BIGINT NOT NULL,
    PRIMARY KEY (block_height, evidence_internal_id),
    CONSTRAINT fk_block_evidences_block FOREIGN KEY (block_height) REFERENCES blocks(height),
    CONSTRAINT fk_block_evidences_evidence FOREIGN KEY (evidence_internal_id) REFERENCES evidences(id)
);

-- Indexes for faster lookups
CREATE INDEX idx_blocks_hash ON blocks(hash);
CREATE INDEX idx_evidences_public_id ON evidences(evidence_id); -- Fast lookup by UUID
CREATE INDEX idx_evidences_hash ON evidences(hash);
CREATE INDEX idx_evidences_homologation ON evidences(homologation_id);

-- 4. IMMUTABILITY ENFORCEMENT (APPEND-ONLY)
-- Function to prevent modifications
CREATE OR REPLACE FUNCTION prevent_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Ledger is append-only. Modification or deletion of committed data is forbidden.';
END;
$$ LANGUAGE plpgsql;

-- Trigger for Blocks: No UPDATE or DELETE allowed
CREATE TRIGGER trg_blocks_immutable
BEFORE UPDATE OR DELETE ON blocks
FOR EACH ROW EXECUTE FUNCTION prevent_modification();

-- Trigger for Block Evidences: No UPDATE or DELETE allowed
CREATE TRIGGER trg_block_evidences_immutable
BEFORE UPDATE OR DELETE ON block_evidences
FOR EACH ROW EXECUTE FUNCTION prevent_modification();

-- Trigger for Evidences: No UPDATE or DELETE allowed
CREATE TRIGGER trg_evidences_immutable
BEFORE UPDATE OR DELETE ON evidences
FOR EACH ROW EXECUTE FUNCTION prevent_modification();
