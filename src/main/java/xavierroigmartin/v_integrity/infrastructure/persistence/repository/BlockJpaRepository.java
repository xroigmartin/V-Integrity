package xavierroigmartin.v_integrity.infrastructure.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import xavierroigmartin.v_integrity.infrastructure.persistence.entity.BlockEntity;

/**
 * JPA Repository for BlockEntity.
 */
@Repository
public interface BlockJpaRepository extends JpaRepository<BlockEntity, Long> {

  Optional<BlockEntity> findByHash(String hash);

  @Query("SELECT b FROM BlockEntity b LEFT JOIN FETCH b.blockEvidences be LEFT JOIN FETCH be.evidence WHERE b.height = :height")
  Optional<BlockEntity> findByHeightWithEvidences(Long height);

  @Query("SELECT MAX(b.height) FROM BlockEntity b")
  Optional<Long> findMaxHeight();
}
