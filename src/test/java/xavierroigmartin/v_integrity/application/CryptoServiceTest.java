package xavierroigmartin.v_integrity.application;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {

    private final CryptoService cryptoService = new CryptoService();

    @Test
    void should_sign_and_verify_correctly() throws Exception {
        // Given
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        KeyPair kp = kpg.generateKeyPair();
        String privateKeyBase64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        String publicKeyBase64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

        byte[] message = "Hello Blockchain".getBytes(StandardCharsets.UTF_8);

        // When
        String signature = cryptoService.signEd25519(message, privateKeyBase64);
        boolean isValid = cryptoService.verifyEd25519(message, signature, publicKeyBase64);

        // Then
        assertNotNull(signature);
        assertTrue(isValid);
    }

    @Test
    void should_fail_verification_if_message_changed() throws Exception {
        // Given
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        KeyPair kp = kpg.generateKeyPair();
        String privateKeyBase64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        String publicKeyBase64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

        byte[] message = "Original".getBytes(StandardCharsets.UTF_8);
        byte[] alteredMessage = "Tampered".getBytes(StandardCharsets.UTF_8);

        // When
        String signature = cryptoService.signEd25519(message, privateKeyBase64);
        boolean isValid = cryptoService.verifyEd25519(alteredMessage, signature, publicKeyBase64);

        // Then
        assertFalse(isValid);
    }
}
