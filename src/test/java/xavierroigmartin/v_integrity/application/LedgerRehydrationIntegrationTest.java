package xavierroigmartin.v_integrity.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import xavierroigmartin.v_integrity.AbstractIntegrationTest;
import xavierroigmartin.v_integrity.domain.Block;
import xavierroigmartin.v_integrity.infrastructure.persistence.entity.BlockEntity;
import xavierroigmartin.v_integrity.infrastructure.persistence.repository.BlockJpaRepository;

class LedgerRehydrationIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private LedgerService ledgerService;

  @Autowired
  private BlockJpaRepository blockRepository;

  @Test
  @Transactional
  @Sql(scripts = "/clean-db.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  void shouldRehydrateChainFromDatabase() {
    // Given: Database has blocks (Genesis)
    var genesis = ledgerService.chain().get(0);
    saveBlockToDb(genesis);
    
    // When: Rehydrate
    var entities = blockRepository.findAll();
    var blocks = entities.stream().map(this::mapToDomain).toList();
    
    ledgerService.initializeChain(blocks);
    
    // Then
    assertThat(ledgerService.chain()).hasSize(1);
    assertThat(ledgerService.chain().get(0).height()).isEqualTo(0);
  }
  
  private void saveBlockToDb(Block block) {
      var entity = new BlockEntity();
      entity.setHeight(block.height());
      entity.setTimestamp(block.timestamp());
      entity.setPreviousHash(block.previousHash());
      entity.setHash(block.hash());
      entity.setProposerNodeId(block.proposerNodeId());
      entity.setSignature(block.signature());
      blockRepository.save(entity);
  }
  
  private Block mapToDomain(BlockEntity entity) {
      return new Block(
          entity.getHeight(),
          entity.getTimestamp(),
          List.of(), // No evidences for genesis
          entity.getPreviousHash(),
          entity.getProposerNodeId(),
          entity.getHash(),
          entity.getSignature()
      );
  }
}
