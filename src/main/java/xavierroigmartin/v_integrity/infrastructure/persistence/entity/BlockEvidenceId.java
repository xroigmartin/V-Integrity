package xavierroigmartin.v_integrity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key for BlockEvidenceEntity.
 */
@Embeddable
public class BlockEvidenceId implements Serializable {

  @Column(name = "block_height")
  private Long blockHeight;

  @Column(name = "evidence_internal_id")
  private Long evidenceInternalId;

  public BlockEvidenceId() {
  }

  public BlockEvidenceId(Long blockHeight, Long evidenceInternalId) {
    this.blockHeight = blockHeight;
    this.evidenceInternalId = evidenceInternalId;
  }

  public Long getBlockHeight() {
    return blockHeight;
  }

  public void setBlockHeight(Long blockHeight) {
    this.blockHeight = blockHeight;
  }

  public Long getEvidenceInternalId() {
    return evidenceInternalId;
  }

  public void setEvidenceInternalId(Long evidenceInternalId) {
    this.evidenceInternalId = evidenceInternalId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BlockEvidenceId that = (BlockEvidenceId) o;
    return Objects.equals(blockHeight, that.blockHeight)
        && Objects.equals(evidenceInternalId, that.evidenceInternalId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(blockHeight, evidenceInternalId);
  }
}
