package xavierroigmartin.v_integrity.interfaces.rest.dto;

/**
 * Response DTO for the synchronization process.
 *
 * @param synced        True if the node is now in sync (or was already in sync).
 * @param appliedBlocks Number of blocks downloaded and applied.
 * @param fromHeight    The height from which synchronization started.
 * @param toHeight      The height reached after synchronization.
 * @param reason        Error message or reason if synced is false.
 */
public record SyncResponse(
    boolean synced,
    int appliedBlocks,
    long fromHeight,
    long toHeight,
    String reason
) {}
