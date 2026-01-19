package xavierroigmartin.v_integrity.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import xavierroigmartin.v_integrity.AbstractIntegrationTest;
import xavierroigmartin.v_integrity.domain.EvidenceRecord;
import xavierroigmartin.v_integrity.infrastructure.persistence.repository.BlockJpaRepository;

class LedgerServicePersistenceIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private LedgerService ledgerService;

  @Autowired
  private BlockJpaRepository blockRepository;

  @Test
  @Transactional
  @Sql(scripts = "/clean-db.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  void commitAsLeader_shouldPersistBlockAndEvidences() {
    // Given: An evidence is submitted
    var evidence = new EvidenceRecord(
        UUID.randomUUID().toString(),
        "HOM-002",
        "RUN-002",
        "report.pdf",
        "PDF",
        "SHA-256",
        "f2d81a260dea8b10036c50a80d27de58d3183c3abcbe84459e452f5d34c177e0",
        12345L,
        "test-user",
        "s3://bucket/report.pdf",
        java.util.List.of("SOC-2"),
        Instant.now()
    );
    ledgerService.submitEvidence(evidence);

    // When: The leader commits a new block
    var committedBlock = ledgerService.commitAsLeader();

    // Then: The block should be found in the database
    var persistedBlockOpt = blockRepository.findById(committedBlock.height());
    
    assertThat(persistedBlockOpt).isPresent();
    var persistedBlock = persistedBlockOpt.get();
    
    assertThat(persistedBlock.getHash()).isEqualTo(committedBlock.hash());
    assertThat(persistedBlock.getHeight()).isEqualTo(1L);
    
    // And it should contain the evidence
    assertThat(persistedBlock.getBlockEvidences()).hasSize(1);
    var persistedEvidence = persistedBlock.getBlockEvidences().iterator().next().getEvidence();
    assertThat(persistedEvidence.getHash()).isEqualTo(evidence.hash());
  }
}
