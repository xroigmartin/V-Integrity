package xavierroigmartin.v_integrity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import xavierroigmartin.v_integrity.infrastructure.persistence.converter.JsonStringListConverter;

/**
 * JPA Entity representing an Evidence Record.
 * Maps to table 'ledger.evidences'.
 */
@Entity
@Table(name = "evidences", schema = "ledger")
public class EvidenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "evidence_id", nullable = false, unique = true)
  private UUID evidenceId;

  @Column(name = "hash", nullable = false, unique = true, length = 64)
  @JdbcTypeCode(Types.CHAR)
  private String hash;

  @Column(name = "hash_algorithm", nullable = false, length = 20)
  private String hashAlgorithm;

  @Column(name = "homologation_id", nullable = false, length = 80)
  private String homologationId;

  @Column(name = "test_run_id", nullable = false, length = 80)
  private String testRunId;

  @Column(name = "artifact_name", nullable = false, length = 255)
  private String artifactName;

  @Column(name = "artifact_type", nullable = false, length = 120)
  private String artifactType;

  @Column(name = "size_bytes")
  private Long sizeBytes;

  @Column(name = "created_by", nullable = false, length = 120)
  private String createdBy;

  @Column(name = "storage_uri", columnDefinition = "TEXT")
  private String storageUri;

  @Column(name = "standards", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  @Convert(converter = JsonStringListConverter.class)
  private List<String> standards;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public EvidenceEntity() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public UUID getEvidenceId() {
    return evidenceId;
  }

  public void setEvidenceId(UUID evidenceId) {
    this.evidenceId = evidenceId;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getHashAlgorithm() {
    return hashAlgorithm;
  }

  public void setHashAlgorithm(String hashAlgorithm) {
    this.hashAlgorithm = hashAlgorithm;
  }

  public String getHomologationId() {
    return homologationId;
  }

  public void setHomologationId(String homologationId) {
    this.homologationId = homologationId;
  }

  public String getTestRunId() {
    return testRunId;
  }

  public void setTestRunId(String testRunId) {
    this.testRunId = testRunId;
  }

  public String getArtifactName() {
    return artifactName;
  }

  public void setArtifactName(String artifactName) {
    this.artifactName = artifactName;
  }

  public String getArtifactType() {
    return artifactType;
  }

  public void setArtifactType(String artifactType) {
    this.artifactType = artifactType;
  }

  public Long getSizeBytes() {
    return sizeBytes;
  }

  public void setSizeBytes(Long sizeBytes) {
    this.sizeBytes = sizeBytes;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getStorageUri() {
    return storageUri;
  }

  public void setStorageUri(String storageUri) {
    this.storageUri = storageUri;
  }

  public List<String> getStandards() {
    return standards;
  }

  public void setStandards(List<String> standards) {
    this.standards = standards;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EvidenceEntity that = (EvidenceEntity) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
