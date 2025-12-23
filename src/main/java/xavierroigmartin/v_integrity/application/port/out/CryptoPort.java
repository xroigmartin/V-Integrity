package xavierroigmartin.v_integrity.application.port.out;

/**
 * Port for cryptographic operations required by the ledger.
 * <p>
 * Defines the contract for signing blocks and verifying signatures using Ed25519.
 * This abstraction allows the application to be agnostic of the underlying crypto library or hardware (HSM).
 */
public interface CryptoPort {

    /**
     * Signs a message using the Ed25519 algorithm.
     *
     * @param message          The byte array of the message (usually a hash) to sign.
     * @param privateKeyBase64 The private key in Base64 format (PKCS#8).
     * @return The generated signature encoded in Base64.
     */
    String signEd25519(byte[] message, String privateKeyBase64);

    /**
     * Verifies an Ed25519 signature against a message and a public key.
     *
     * @param message         The original message (bytes) that was signed.
     * @param signatureBase64 The signature to verify, in Base64 format.
     * @param publicKeyBase64 The signer's public key in Base64 format (X.509).
     * @return true if the signature is valid, false otherwise.
     */
    boolean verifyEd25519(byte[] message, String signatureBase64, String publicKeyBase64);
}
