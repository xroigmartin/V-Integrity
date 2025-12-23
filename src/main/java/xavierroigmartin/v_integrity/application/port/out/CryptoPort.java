package xavierroigmartin.v_integrity.application.port.out;

public interface CryptoPort {
    String signEd25519(byte[] message, String privateKeyBase64);
    boolean verifyEd25519(byte[] message, String signatureBase64, String publicKeyBase64);
}
