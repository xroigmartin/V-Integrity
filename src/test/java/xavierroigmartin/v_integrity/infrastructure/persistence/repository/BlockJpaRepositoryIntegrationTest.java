package xavierroigmartin.v_integrity.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import xavierroigmartin.v_integrity.AbstractIntegrationTest;
import xavierroigmartin.v_integrity.infrastructure.persistence.entity.BlockEntity;
import xavierroigmartin.v_integrity.infrastructure.persistence.entity.BlockEvidenceEntity;
import xavierroigmartin.v_integrity.infrastructure.persistence.entity.EvidenceEntity;

class BlockJpaRepositoryIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private BlockJpaRepository blockRepository;

  @Autowired
  private EvidenceJpaRepository evidenceRepository;

  @Test
  @Transactional
  void shouldSaveAndRetrieveBlockWithEvidences() {
    // Given: An Evidence
    EvidenceEntity evidence = new EvidenceEntity();
    evidence.setEvidenceId(UUID.randomUUID());
    // SHA-256 hash (64 chars)
    evidence.setHash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"); 
    evidence.setHashAlgorithm("SHA-256");
    evidence.setHomologationId("HOM-001");
    evidence.setTestRunId("RUN-001");
    evidence.setArtifactName("test.log");
    evidence.setArtifactType("LOG");
    evidence.setCreatedBy("tester");
    evidence.setCreatedAt(Instant.now());
    evidence.setStandards(List.of("ISO-27001", "GDPR")); // JSONB test

    // Save evidence first (since we are manually building the relationship for now)
    evidence = evidenceRepository.save(evidence);

    // Given: A Block
    BlockEntity block = new BlockEntity();
    block.setHeight(1L);
    block.setTimestamp(Instant.now());
    // 64 chars
    block.setPreviousHash("0000000000000000000000000000000000000000000000000000000000000000");
    // 64 chars (Fixed length)
    block.setHash("1234567890123456789012345678901234567890123456789012345678901234");
    block.setProposerNodeId("node-1");
    block.setSignature("signature_base64");

    // Given: The Link
    BlockEvidenceEntity link = new BlockEvidenceEntity(block, evidence);
    block.getBlockEvidences().add(link);

    // When: Save Block
    blockRepository.save(block);

    // Then: Retrieve and Verify
    BlockEntity retrievedBlock = blockRepository.findByHeightWithEvidences(1L).orElseThrow();
    
    assertThat(retrievedBlock.getHash()).isEqualTo(block.getHash());
    assertThat(retrievedBlock.getBlockEvidences()).hasSize(1);
    
    EvidenceEntity retrievedEvidence = retrievedBlock.getBlockEvidences().iterator().next().getEvidence();
    assertThat(retrievedEvidence.getEvidenceId()).isEqualTo(evidence.getEvidenceId());
    assertThat(retrievedEvidence.getStandards()).containsExactly("ISO-27001", "GDPR");
    assertThat(retrievedEvidence.getId()).isNotNull(); // Surrogate key generated
  }
}
