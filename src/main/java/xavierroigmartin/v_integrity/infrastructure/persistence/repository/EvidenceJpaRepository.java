package xavierroigmartin.v_integrity.infrastructure.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xavierroigmartin.v_integrity.infrastructure.persistence.entity.EvidenceEntity;

/**
 * JPA Repository for EvidenceEntity.
 */
@Repository
public interface EvidenceJpaRepository extends JpaRepository<EvidenceEntity, Long> {

  Optional<EvidenceEntity> findByHash(String hash);
}
