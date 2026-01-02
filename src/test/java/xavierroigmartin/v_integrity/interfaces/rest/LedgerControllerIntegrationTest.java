package xavierroigmartin.v_integrity.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import xavierroigmartin.v_integrity.application.LedgerService;
import xavierroigmartin.v_integrity.application.port.out.CryptoPort;
import xavierroigmartin.v_integrity.application.port.out.HashingPort;
import xavierroigmartin.v_integrity.domain.Block;
import xavierroigmartin.v_integrity.domain.EvidenceRecord;
import xavierroigmartin.v_integrity.interfaces.rest.dto.EvidenceRequest;
import xavierroigmartin.v_integrity.interfaces.rest.dto.SyncRequest;
import xavierroigmartin.v_integrity.interfaces.rest.dto.VerifyRequest;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class LedgerControllerIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private LedgerService ledgerService;

  @Autowired
  private CryptoPort crypto;

  @Autowired
  private HashingPort hashing;

  @Value("${ledger.node.privateKeyBase64}")
  private String privateKeyBase64;

  @Value("${ledger.node.nodeId}")
  private String nodeId;

  private static WireMockServer wireMockServer;

  @BeforeAll
  static void startWireMock() {
    wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    wireMockServer.start();
    WireMock.configureFor(wireMockServer.port());
  }

  @AfterAll
  static void stopWireMock() {
    wireMockServer.stop();
  }

  @BeforeEach
  void setUp() {
    // Reset ledger state if possible or ensure clean state
    // For this PoC, we might rely on restarting context or just appending
  }

  @Test
  void shouldSubmitEvidenceSuccessfully() {
    EvidenceRequest request = new EvidenceRequest(
        "H-123",
        "TR-456",
        "report.pdf",
        "PDF",
        "SHA-256",
        "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e",
        1024L,
        "user-1",
        "s3://bucket/report.pdf",
        List.of("ISO-27001")
    );

    ResponseEntity<Map> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/api/evidences",
        request,
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).containsKey("evidence");
    
    Map<String, Object> evidence = (Map<String, Object>) response.getBody().get("evidence");
    assertThat(evidence.get("homologationId")).isEqualTo("H-123");
  }

  @Test
  void shouldReturnBadRequestForInvalidEvidence() {
    EvidenceRequest invalidRequest = new EvidenceRequest(
        null, 
        "TR-456",
        "report.pdf",
        "PDF",
        "SHA-256",
        "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e",
        1024L,
        "user-1",
        "s3://bucket/report.pdf",
        List.of("ISO-27001")
    );

    ResponseEntity<Map> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/api/evidences",
        invalidRequest,
        Map.class
    );

    // Updated: Now strictly expects BAD_REQUEST (400) thanks to GlobalExceptionHandler
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldReturnFormattedErrorForValidationFailure() {
    // Request with missing mandatory fields (hashAlgorithm is null)
    EvidenceRequest invalidRequest = new EvidenceRequest(
        "H-123", "TR-456", "report.pdf", "PDF", null, "hash", 1024L, "user", "uri", List.of()
    );

    ResponseEntity<Map> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/api/evidences",
        invalidRequest,
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    // Expecting RFC 7807 Problem Details structure
    assertThat(response.getBody()).containsKey("detail");
    assertThat(response.getBody()).containsKey("title");
    assertThat(response.getBody()).containsKey("status");
    
    String detail = (String) response.getBody().get("detail");
    assertThat(detail).contains("Validation failed");
    assertThat(detail).contains("hashAlgorithm");
  }

  @Test
  void shouldReturnBadRequestWhenCommittingWithoutEvidences() {
    // Ensure mempool is empty (might need a way to clear it or assume test order/isolation)
    // In this simple PoC test, we just try to commit. If mempool is empty, it throws IllegalStateException.
    // If not empty, it commits. To be safe, we can try to commit until empty or just assert 201 or 400.
    // But specifically, we want to test the exception mapping.
    
    // Let's try to commit. If it succeeds (201), we do it again until it fails (400) because mempool is empty.
    ResponseEntity<Map> response = restTemplate.postForEntity(
        "/api/blocks/commit",
        null,
        Map.class
    );

    if (response.getStatusCode() == HttpStatus.CREATED) {
       // Try again, now mempool should be empty
       response = restTemplate.postForEntity(
          "/api/blocks/commit",
          null,
          Map.class
      );
    }

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    // Expecting RFC 7807 Problem Details structure
    assertThat(response.getBody()).containsKey("detail");
  }

  @Test
  void shouldRetrieveChain() {
    ResponseEntity<Map> response = restTemplate.getForEntity(
        "http://localhost:" + port + "/api/chain",
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsKey("chain");
    assertThat(response.getBody()).containsKey("length");
  }

  @Test
  void shouldVerifyEvidence() {
    // 1. Submit evidence
    EvidenceRequest request = new EvidenceRequest(
        "H-999",
        "TR-999",
        "audit.log",
        "LOG",
        "SHA-256",
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        0L,
        "system",
        "local://audit.log",
        List.of()
    );
    restTemplate.postForEntity("/api/evidences", request, Map.class);

    // 2. Commit block
    try {
      restTemplate.postForEntity("/api/blocks/commit", null, Map.class);
    } catch (Exception e) {
      // Ignore
    }

    // 3. Verify
    VerifyRequest verifyReq = new VerifyRequest("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    ResponseEntity<Map> response = restTemplate.postForEntity(
        "/api/verify",
        verifyReq,
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsKey("verified");
  }

  @Test
  void shouldCommitAndVerifyEvidence() {
    // 1. Submit evidence
    String hash = "aaaaa6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e";
    EvidenceRequest request = new EvidenceRequest(
        "H-COMMIT", "TR-COMMIT", "commit.pdf", "PDF", "SHA-256", hash, 1024L, "user-commit", "s3://bucket/commit.pdf", List.of()
    );
    restTemplate.postForEntity("/api/evidences", request, Map.class);

    // 2. Commit block (as leader)
    ResponseEntity<Map> commitResponse = restTemplate.postForEntity(
        "/api/blocks/commit",
        null,
        Map.class
    );
    assertThat(commitResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(commitResponse.getBody()).containsKey("block");

    // 3. Verify by hash (should be found now)
    ResponseEntity<Map> verifyResponse = restTemplate.getForEntity(
        "/api/evidences/hash/" + hash,
        Map.class
    );
    assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(verifyResponse.getBody()).containsEntry("found", true);
    assertThat(verifyResponse.getBody()).containsKey("proof");
    
    // 4. Verify endpoint
    VerifyRequest verifyReq = new VerifyRequest(hash);
    ResponseEntity<Map> verifyEndpointResponse = restTemplate.postForEntity(
        "/api/verify",
        verifyReq,
        Map.class
    );
    assertThat(verifyEndpointResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(verifyEndpointResponse.getBody()).containsEntry("verified", true);
  }

  @Test
  void shouldGetLatestBlock() {
    ResponseEntity<Map> response = restTemplate.getForEntity(
        "http://localhost:" + port + "/api/blocks/latest",
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsKey("height");
    assertThat(response.getBody()).containsKey("hash");
  }

  @Test
  void shouldGetBlocksRange() {
    ResponseEntity<List> response = restTemplate.getForEntity(
        "http://localhost:" + port + "/api/blocks?fromHeight=0&limit=10",
        List.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotEmpty();
  }

  @Test
  void shouldTriggerSync() {
    SyncRequest syncRequest = new SyncRequest("http://localhost:" + wireMockServer.port());
    
    // Mock peer response for sync if needed, but for now just trigger endpoint
    // Since we don't have a real peer, it might fail or return empty sync, but endpoint should be reachable
    ResponseEntity<Map> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/api/sync",
        syncRequest,
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldGetMempool() {
    ResponseEntity<Map> response = restTemplate.getForEntity(
        "http://localhost:" + port + "/api/mempool",
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsKey("size");
    assertThat(response.getBody()).containsKey("mempool");
  }

  @Test
  void shouldRejectMalformedReplicatedBlock() {
    // Block with null fields or invalid structure that causes processing error
    Block block = new Block(
        100L,
        Instant.now(),
        List.of(),
        "prevHash",
        "node-2",
        "hash",
        "signature"
    );

    ResponseEntity<Map> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/api/blocks/receive",
        block,
        Map.class
    );

    // Updated: Now strictly expects BAD_REQUEST (400)
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldAcceptValidReplicatedBlock() {
    // 1. Get current chain state
    Block latest = ledgerService.latestBlock();
    long nextHeight = latest.height() + 1;
    String prevHash = latest.hash();
    // Truncate to millis to match Jackson serialization precision
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    List<EvidenceRecord> evidences = List.of(); // Empty for simplicity

    // 2. Calculate canonical hash (same logic as LedgerService)
    String canonical = canonicalBlockFields(nextHeight, now, evidences, prevHash, nodeId);
    String hashHex = hashing.sha256Hex(canonical);
    byte[] hashBytes = hexToBytes(hashHex);

    // 3. Sign
    String signature = crypto.signEd25519(hashBytes, privateKeyBase64);

    // 4. Create valid block
    Block validBlock = new Block(
        nextHeight,
        now,
        evidences,
        prevHash,
        nodeId,
        hashHex,
        signature
    );

    // 5. Send
    ResponseEntity<Map> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/api/blocks/receive",
        validBlock,
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.getBody()).containsKey("accepted");
    assertThat(response.getBody().get("accepted")).isEqualTo(true);
  }

  @Test
  void shouldValidateChain() {
    ResponseEntity<Map> response = restTemplate.getForEntity(
        "http://localhost:" + port + "/api/validate",
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsKey("valid");
  }

  @Test
  void shouldGetEvidenceByHash() {
    // 1. Submit evidence
    String hash = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
    EvidenceRequest request = new EvidenceRequest(
        "H-TEST", "TR-TEST", "test.log", "LOG", "SHA-256", hash, 0L, "tester", "uri", List.of()
    );
    restTemplate.postForEntity("/api/evidences", request, Map.class);

    // 2. Query by hash
    ResponseEntity<Map> response = restTemplate.getForEntity(
        "http://localhost:" + port + "/api/evidences/hash/" + hash,
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    // Might be found=false if not mined yet, but endpoint works
    assertThat(response.getBody()).containsKey("found");
  }

  // Helper to replicate canonicalization logic for test block creation
  private String canonicalBlockFields(long height, Instant ts, List<EvidenceRecord> evidences,
      String previousHash, String proposer) {
    StringBuilder sb = new StringBuilder();
    sb.append("height=").append(height).append("|");
    sb.append("ts=").append(ts.toString()).append("|");
    sb.append("prev=").append(previousHash).append("|");
    sb.append("proposer=").append(proposer).append("|");
    sb.append("evidences=");

    List<EvidenceRecord> sorted = new ArrayList<>(evidences);
    sorted.sort(Comparator.comparing(EvidenceRecord::evidenceId));

    for (EvidenceRecord e : sorted) {
      sb.append(e.evidenceId()).append(",");
      sb.append(e.homologationId()).append(",");
      sb.append(e.testRunId()).append(",");
      sb.append(e.artifactName()).append(",");
      sb.append(e.artifactType()).append(",");
      sb.append(e.hashAlgorithm()).append(",");
      sb.append(e.hash()).append(",");
      sb.append(e.sizeBytes() == null ? "" : e.sizeBytes()).append(",");
      sb.append(e.createdBy()).append(",");
      sb.append(e.storageUri() == null ? "" : e.storageUri()).append(",");
      sb.append(e.createdAt().toString()).append(",");

      List<String> std = e.standards() == null ? List.of() : e.standards();
      List<String> stdSorted = new ArrayList<>(std);
      stdSorted.sort(String::compareTo);
      sb.append(String.join("+", stdSorted));

      sb.append(";");
    }
    return sb.toString();
  }

  private static byte[] hexToBytes(String hex) {
    int len = hex.length();
    byte[] out = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
    }
    return out;
  }
}
