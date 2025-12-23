package xavierroigmartin.v_integrity.infrastructure.adapter;

import org.springframework.stereotype.Component;
import xavierroigmartin.v_integrity.application.port.out.CryptoPort;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Implementation of {@link CryptoPort} using Java's standard security libraries (JCA).
 * <p>
 * Uses "Ed25519" algorithm for digital signatures, which is standard in modern blockchains
 * for its high performance and security.
 */
@Component
public class CryptoAdapter implements CryptoPort {

    @Override
    public String signEd25519(byte[] message, String privateKeyBase64) {
        try {
            byte[] pkcs8 = Base64.getDecoder().decode(privateKeyBase64);
            PrivateKey privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));

            Signature sig = Signature.getInstance("Ed25519");
            sig.initSign(privateKey);
            sig.update(message);

            byte[] signature = sig.sign();
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new IllegalStateException("Error signing with Ed25519", e);
        }
    }

    @Override
    public boolean verifyEd25519(byte[] message, String signatureBase64, String publicKeyBase64) {
        try {
            byte[] x509 = Base64.getDecoder().decode(publicKeyBase64);
            PublicKey publicKey = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(x509));

            Signature sig = Signature.getInstance("Ed25519");
            sig.initVerify(publicKey);
            sig.update(message);

            byte[] signature = Base64.getDecoder().decode(signatureBase64);
            return sig.verify(signature);
        } catch (Exception e) {
            // If any error occurs during verification (bad format, etc.), consider signature invalid.
            return false;
        }
    }
}
