package xavierroigmartin.v_integrity.infrastructure.adapter;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xavierroigmartin.v_integrity.application.LedgerService;
import xavierroigmartin.v_integrity.domain.Block;
import xavierroigmartin.v_integrity.domain.EvidenceRecord;
import xavierroigmartin.v_integrity.infrastructure.exception.RehydrationFailedException;
import xavierroigmartin.v_integrity.infrastructure.persistence.entity.BlockEntity;
import xavierroigmartin.v_integrity.infrastructure.persistence.entity.EvidenceEntity;
import xavierroigmartin.v_integrity.infrastructure.persistence.repository.BlockJpaRepository;

/**
 * Adapter responsible for rehydrating the in-memory ledger from the database on startup.
 * Implements ApplicationRunner to execute after the Spring context is fully loaded.
 */
@Component
public class LedgerRehydrationAdapter implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(LedgerRehydrationAdapter.class);

  private final BlockJpaRepository blockRepository;
  private final LedgerService ledgerService;

  public LedgerRehydrationAdapter(BlockJpaRepository blockRepository, LedgerService ledgerService) {
    this.blockRepository = blockRepository;
    this.ledgerService = ledgerService;
  }

  @Override
  @Transactional(readOnly = true)
  public void run(ApplicationArguments args) throws Exception {
    logger.info("Starting Ledger Rehydration from Database...");

    // 1. Load all blocks ordered by height
    // Using the optimized query to fetch evidences eagerly would be better for performance,
    // but findAll(Sort) is simpler for now. Given N+1 issue, we might want to optimize later.
    // For PoC, standard findAll is acceptable, or we can use a custom query.
    // Let's use findAll for simplicity and rely on Batch fetching if configured, or accept N+1 for startup.
    var blockEntities = blockRepository.findAll(Sort.by("height"));

    if (blockEntities.isEmpty()) {
      logger.info("Database is empty. Starting with Genesis block.");
      return;
    }

    logger.info("Found {} blocks in database. Reconstructing domain objects...", blockEntities.size());

    // 2. Map to Domain Objects
    var blocks = blockEntities.stream()
        .map(this::mapToDomain)
        .collect(Collectors.toList());

    // 3. Initialize Ledger Service
    try {
      ledgerService.initializeChain(blocks);
      logger.info("Ledger Rehydration completed successfully. Chain height: {}", ledgerService.latestBlock().height());
    } catch (Exception e) {
      logger.error("CRITICAL: Ledger Rehydration FAILED. The database might be corrupt.", e);
      // Fail-fast: Stop the application if the ledger is corrupt
      throw new RehydrationFailedException("Failed to rehydrate ledger", e);
    }
  }

  private Block mapToDomain(BlockEntity entity) {
    var evidences = entity.getBlockEvidences().stream()
        .map(be -> mapToDomain(be.getEvidence()))
        .collect(Collectors.toList());

    return new Block(
        entity.getHeight(),
        entity.getTimestamp(),
        evidences,
        entity.getPreviousHash(),
        entity.getProposerNodeId(),
        entity.getHash(),
        entity.getSignature()
    );
  }

  private EvidenceRecord mapToDomain(EvidenceEntity entity) {
    return new EvidenceRecord(
        entity.getEvidenceId().toString(),
        entity.getHomologationId(),
        entity.getTestRunId(),
        entity.getArtifactName(),
        entity.getArtifactType(),
        entity.getHashAlgorithm(),
        entity.getHash(),
        entity.getSizeBytes(),
        entity.getCreatedBy(),
        entity.getStorageUri(),
        entity.getStandards(),
        entity.getCreatedAt()
    );
  }
}
