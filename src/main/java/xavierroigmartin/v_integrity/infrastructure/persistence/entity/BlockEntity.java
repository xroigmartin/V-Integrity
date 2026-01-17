package xavierroigmartin.v_integrity.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.sql.Types;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * JPA Entity representing a Block in the ledger.
 * Maps to table 'ledger.blocks'.
 */
@Entity
@Table(name = "blocks", schema = "ledger")
public class BlockEntity {

  @Id
  @Column(name = "height", nullable = false)
  private Long height;

  @Column(name = "timestamp", nullable = false)
  private Instant timestamp;

  @Column(name = "previous_hash", nullable = false, length = 64)
  @JdbcTypeCode(Types.CHAR)
  private String previousHash;

  @Column(name = "hash", nullable = false, unique = true, length = 64)
  @JdbcTypeCode(Types.CHAR)
  private String hash;

  @Column(name = "proposer_node_id", nullable = false, length = 100)
  private String proposerNodeId;

  @Column(name = "signature", nullable = false, columnDefinition = "TEXT")
  private String signature;

  @OneToMany(mappedBy = "block", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<BlockEvidenceEntity> blockEvidences = new HashSet<>();

  public BlockEntity() {
  }

  public Long getHeight() {
    return height;
  }

  public void setHeight(Long height) {
    this.height = height;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public String getPreviousHash() {
    return previousHash;
  }

  public void setPreviousHash(String previousHash) {
    this.previousHash = previousHash;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getProposerNodeId() {
    return proposerNodeId;
  }

  public void setProposerNodeId(String proposerNodeId) {
    this.proposerNodeId = proposerNodeId;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public Set<BlockEvidenceEntity> getBlockEvidences() {
    return blockEvidences;
  }

  public void setBlockEvidences(Set<BlockEvidenceEntity> blockEvidences) {
    this.blockEvidences = blockEvidences;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BlockEntity that = (BlockEntity) o;
    return Objects.equals(height, that.height);
  }

  @Override
  public int hashCode() {
    return Objects.hash(height);
  }
}
