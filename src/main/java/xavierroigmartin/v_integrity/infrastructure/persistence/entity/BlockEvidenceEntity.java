package xavierroigmartin.v_integrity.infrastructure.persistence.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * JPA Entity representing the many-to-many relationship between Blocks and Evidences.
 * Maps to table 'ledger.block_evidences'.
 */
@Entity
@Table(name = "block_evidences", schema = "ledger")
public class BlockEvidenceEntity {

  @EmbeddedId
  private BlockEvidenceId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("blockHeight")
  @JoinColumn(name = "block_height")
  private BlockEntity block;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("evidenceInternalId")
  @JoinColumn(name = "evidence_internal_id")
  private EvidenceEntity evidence;

  public BlockEvidenceEntity() {
  }

  public BlockEvidenceEntity(BlockEntity block, EvidenceEntity evidence) {
    this.block = block;
    this.evidence = evidence;
    this.id = new BlockEvidenceId(block.getHeight(), evidence.getId());
  }

  public BlockEvidenceId getId() {
    return id;
  }

  public void setId(BlockEvidenceId id) {
    this.id = id;
  }

  public BlockEntity getBlock() {
    return block;
  }

  public void setBlock(BlockEntity block) {
    this.block = block;
  }

  public EvidenceEntity getEvidence() {
    return evidence;
  }

  public void setEvidence(EvidenceEntity evidence) {
    this.evidence = evidence;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BlockEvidenceEntity that = (BlockEvidenceEntity) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
