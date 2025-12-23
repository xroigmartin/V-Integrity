package xavierroigmartin.v_integrity.application;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class CryptoService {

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
            throw new IllegalStateException("Error firmando Ed25519", e);
        }
    }

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
            return false;
        }
    }
}