package xavierroigmartin.v_integrity.interfaces.rest.dto;

/**
 * Request DTO for triggering synchronization manually.
 *
 * @param sourcePeerUrl Optional URL of the peer to sync from. If null, uses the first configured peer.
 */
public record SyncRequest(
    String sourcePeerUrl
) {}
