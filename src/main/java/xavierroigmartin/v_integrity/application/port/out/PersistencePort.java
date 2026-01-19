package xavierroigmartin.v_integrity.application.port.out;

import xavierroigmartin.v_integrity.domain.Block;

/**
 * Output port for persisting ledger data.
 */
public interface PersistencePort {

  /**
   * Persists a confirmed block and its evidences to the storage.
   * This operation must be idempotent and transactional.
   *
   * @param block The domain block to persist.
   */
  void persistBlock(Block block);
}
