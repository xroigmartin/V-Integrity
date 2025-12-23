package xavierroigmartin.v_integrity.infrastructure.adapter;

import org.springframework.stereotype.Component;
import xavierroigmartin.v_integrity.application.port.out.HashingPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Implementation of {@link HashingPort} using Java's {@link MessageDigest}.
 * <p>
 * Provides standard SHA-256 hashing functionality.
 */
@Component
public class HashingAdapter implements HashingPort {
    
    @Override
    public String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Could not compute SHA-256", e);
        }
    }

    @Override
    public byte[] sha256Bytes(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Could not compute SHA-256", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
