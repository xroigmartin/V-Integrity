package xavierroigmartin.v_integrity.application.port.out;

/**
 * Port for hashing operations.
 * <p>
 * Blockchain relies heavily on cryptographic hashing (SHA-256) to link blocks and ensure data integrity.
 */
public interface HashingPort {

    /**
     * Computes the SHA-256 hash of a string input and returns it as a hexadecimal string.
     *
     * @param input The input string to hash.
     * @return The SHA-256 hash in lowercase hexadecimal format (64 characters).
     */
    String sha256Hex(String input);

    /**
     * Computes the SHA-256 hash of a string input and returns the raw bytes.
     *
     * @param input The input string to hash.
     * @return The SHA-256 hash as a byte array (32 bytes).
     */
    byte[] sha256Bytes(String input);
}
