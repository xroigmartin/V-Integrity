package xavierroigmartin.v_integrity.application;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HashingServiceTest {

    private final HashingService hashingService = new HashingService();

    @Test
    void should_generate_deterministic_sha256() {
        // Given
        String input = "test";
        // SHA-256 of "test" is 9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08

        // When
        String hash1 = hashingService.sha256Hex(input);
        String hash2 = hashingService.sha256Hex(input);

        // Then
        assertEquals(hash1, hash2);
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", hash1);
    }
}
