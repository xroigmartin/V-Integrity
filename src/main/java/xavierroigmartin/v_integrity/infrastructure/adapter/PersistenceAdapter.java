package xavierroigmartin.v_integrity.infrastructure.adapter;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xavierroigmartin.v_integrity.application.port.out.PersistencePort;
import xavierroigmartin.v_integrity.domain.Block;
import xavierroigmartin.v_integrity.domain.EvidenceRecord;
import xavierroigmartin.v_integrity.domain.exception.InvalidBlockException;
import xavierroigmartin.v_integrity.infrastructure.persistence.entity.BlockEntity;
import xavierroigmartin.v_integrity.infrastructure.persistence.entity.BlockEvidenceEntity;
import xavierroigmartin.v_integrity.infrastructure.persistence.entity.EvidenceEntity;
import xavierroigmartin.v_integrity.infrastructure.persistence.repository.BlockJpaRepository;
import xavierroigmartin.v_integrity.infrastructure.persistence.repository.EvidenceJpaRepository;

/**
 * Adapter implementation for persisting ledger data using JPA.
 */
@Component
public class PersistenceAdapter implements PersistencePort {

  private static final Logger logger = LoggerFactory.getLogger(PersistenceAdapter.class);

  private final BlockJpaRepository blockRepository;
  private final EvidenceJpaRepository evidenceRepository;

  public PersistenceAdapter(BlockJpaRepository blockRepository,
      EvidenceJpaRepository evidenceRepository) {
    this.blockRepository = blockRepository;
    this.evidenceRepository = evidenceRepository;
  }

  @Override
  @Transactional
  public void persistBlock(Block block) {
    logger.info("Persisting block height={} hash={}", block.height(), block.hash());

    // 1. Check idempotency for Block
    Optional<BlockEntity> existingBlock = blockRepository.findById(block.height());
    if (existingBlock.isPresent()) {
      if (existingBlock.get().getHash().equals(block.hash())) {
        logger.info("Block height={} already exists with same hash. Skipping persistence.", block.height());
        return;
      } else {
        logger.error("CRITICAL: Block height={} exists but hash mismatch! Existing={}, New={}",
            block.height(), existingBlock.get().getHash(), block.hash());
        throw new InvalidBlockException("Block height collision with different hash. Possible fork or corruption.");
      }
    }

    // 2. Map Domain Block to Entity
    BlockEntity blockEntity = new BlockEntity();
    blockEntity.setHeight(block.height());
    // Fix: Block.timestamp() returns Instant, so use it directly
    blockEntity.setTimestamp(block.timestamp());
    blockEntity.setPreviousHash(block.previousHash());
    blockEntity.setHash(block.hash());
    blockEntity.setProposerNodeId(block.proposerNodeId());
    blockEntity.setSignature(block.signature());

    // 3. Process Evidences (Find or Create)
    Set<BlockEvidenceEntity> blockEvidences = block.evidences().stream()
        .map(evidenceRecord -> {
          EvidenceEntity evidenceEntity = findOrCreateEvidence(evidenceRecord);
          return new BlockEvidenceEntity(blockEntity, evidenceEntity);
        })
        .collect(Collectors.toSet());

    blockEntity.setBlockEvidences(blockEvidences);

    // 4. Save Block (Cascades to BlockEvidenceEntity)
    blockRepository.save(blockEntity);
    logger.info("Block height={} persisted successfully.", block.height());
  }

  private EvidenceEntity findOrCreateEvidence(EvidenceRecord record) {
    return evidenceRepository.findByHash(record.hash())
        .orElseGet(() -> {
          EvidenceEntity newEntity = new EvidenceEntity();
          // Use the ID from the record if present, otherwise generate (though domain should usually have it)
          // In this PoC, EvidenceRecord has an ID.
          newEntity.setEvidenceId(record.evidenceId() != null ? UUID.fromString(record.evidenceId()) : UUID.randomUUID());
          newEntity.setHash(record.hash());
          newEntity.setHashAlgorithm(record.hashAlgorithm());
          newEntity.setHomologationId(record.homologationId());
          newEntity.setTestRunId(record.testRunId());
          newEntity.setArtifactName(record.artifactName());
          newEntity.setArtifactType(record.artifactType());
          newEntity.setSizeBytes(record.sizeBytes());
          newEntity.setCreatedBy(record.createdBy());
          newEntity.setStorageUri(record.storageUri());
          newEntity.setStandards(record.standards());
          newEntity.setCreatedAt(record.createdAt());
          
          return evidenceRepository.save(newEntity);
        });
  }
}
