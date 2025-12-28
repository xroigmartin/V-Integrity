package xavierroigmartin.v_integrity.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xavierroigmartin.v_integrity.application.port.out.CryptoPort;
import xavierroigmartin.v_integrity.application.port.out.HashingPort;
import xavierroigmartin.v_integrity.application.port.out.LogPort;
import xavierroigmartin.v_integrity.application.port.out.NodeConfigurationPort;
import xavierroigmartin.v_integrity.application.port.out.ReplicationPort;
import xavierroigmartin.v_integrity.domain.Block;
import xavierroigmartin.v_integrity.domain.EvidenceRecord;
import xavierroigmartin.v_integrity.infrastructure.adapter.CryptoAdapter;
import xavierroigmartin.v_integrity.infrastructure.adapter.HashingAdapter;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private NodeConfigurationPort nodeConfig;
    @Mock
    private ReplicationPort replication;
    @Mock
    private LogPort logger;

    private LedgerService ledgerService;
    private CryptoPort crypto;
    private HashingPort hashing;

    private String myNodeId = "node-1";
    private String myPrivateKey;
    private String myPublicKey;

    @BeforeEach
    void setUp() throws Exception {
        // Use real adapters for logic-heavy ports
        crypto = new CryptoAdapter();
        hashing = new HashingAdapter();

        // Generate real keys for testing
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        KeyPair kp = kpg.generateKeyPair();
        myPrivateKey = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        myPublicKey = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

        // Default mock behavior
        lenient().when(nodeConfig.getNodeId()).thenReturn(myNodeId);
        lenient().when(nodeConfig.getAllowedNodePublicKeys()).thenReturn(Map.of(myNodeId, myPublicKey));
        
        ledgerService = new LedgerService(nodeConfig, hashing, crypto, replication, logger);
    }

    @Test
    void should_start_with_genesis_block() {
        List<Block> chain = ledgerService.chain();
        assertEquals(1, chain.size());
        assertEquals(0, chain.get(0).height());
        assertEquals("GENESIS", chain.get(0).proposerNodeId());
    }

    @Test
    void should_submit_evidence_to_mempool() {
        EvidenceRecord evidence = createSampleEvidence();
        EvidenceRecord submitted = ledgerService.submitEvidence(evidence);

        assertEquals(1, ledgerService.mempool().size());
        assertEquals(evidence.evidenceId(), submitted.evidenceId());
        assertEquals("SHA-256", submitted.hashAlgorithm()); // Normalized
        
        // Verify logging
        verify(logger).logBusinessEvent(eq("EVIDENCE_SUBMITTED"), anyMap());
    }

    @Test
    void should_commit_block_as_leader() {
        // Given
        when(nodeConfig.isLeader()).thenReturn(true);
        when(nodeConfig.getPrivateKeyBase64()).thenReturn(myPrivateKey);
        when(nodeConfig.getPeers()).thenReturn(List.of("http://peer1"));

        ledgerService.submitEvidence(createSampleEvidence());

        // When
        Block block = ledgerService.commitAsLeader();

        // Then
        assertNotNull(block);
        assertEquals(1, block.height());
        assertEquals(1, block.evidences().size());
        assertEquals(myNodeId, block.proposerNodeId());
        
        // Verify replication was called
        verify(replication).replicateBlockToPeers(eq(block), anyList());
        
        // Verify logging
        verify(logger).logBusinessEvent(eq("BLOCK_COMMITTED"), anyMap());
        
        // Mempool should be empty
        assertTrue(ledgerService.mempool().isEmpty());
        assertEquals(2, ledgerService.chain().size()); // Genesis + 1
    }

    @Test
    void should_fail_commit_if_not_leader() {
        when(nodeConfig.isLeader()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> ledgerService.commitAsLeader());
    }

    @Test
    void should_accept_valid_replicated_block() {
        // 1. Create a valid block (simulating another leader or self)
        when(nodeConfig.isLeader()).thenReturn(true);
        when(nodeConfig.getPrivateKeyBase64()).thenReturn(myPrivateKey);
        
        ledgerService.submitEvidence(createSampleEvidence());
        Block validBlock = ledgerService.commitAsLeader();

        // Reset service to simulate a follower receiving this block
        LedgerService followerService = new LedgerService(nodeConfig, hashing, crypto, replication, logger);
        
        // When
        followerService.acceptReplicatedBlock(validBlock);

        // Then
        assertEquals(2, followerService.chain().size());
        assertEquals(validBlock, followerService.chain().get(1));
        
        // Verify logging: 1 submit + 1 commit + 1 accept = 3 times
        verify(logger, times(3)).logBusinessEvent(anyString(), anyMap());
    }

    @Test
    void should_reject_invalid_signature_block() {
        // Given
        when(nodeConfig.isLeader()).thenReturn(true);
        when(nodeConfig.getPrivateKeyBase64()).thenReturn(myPrivateKey);
        ledgerService.submitEvidence(createSampleEvidence());
        Block validBlock = ledgerService.commitAsLeader();

        // Create a tampered block with same signature but different hash/content
        Block tamperedBlock = new Block(
                validBlock.height(),
                validBlock.timestamp(),
                validBlock.evidences(),
                validBlock.previousHash(),
                validBlock.proposerNodeId(),
                "badhash", // Invalid hash
                validBlock.signature()
        );

        LedgerService followerService = new LedgerService(nodeConfig, hashing, crypto, replication, logger);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> followerService.acceptReplicatedBlock(tamperedBlock));
        
        // Verify error logging
        verify(logger).logBusinessError(eq("INVALID_BLOCK_HASH"), anyString(), anyMap());
    }

    private EvidenceRecord createSampleEvidence() {
        return new EvidenceRecord(
                UUID.randomUUID().toString(),
                "HOM-123",
                "RUN-456",
                "log.txt",
                "LOG",
                "SHA-256",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", // Empty SHA-256
                1024L,
                "user1",
                "s3://bucket/log.txt",
                List.of("ISO-27001"),
                Instant.now()
        );
    }
}
