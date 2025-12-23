package xavierroigmartin.v_integrity.interfaces.rest;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import xavierroigmartin.v_integrity.application.LedgerService;
import xavierroigmartin.v_integrity.application.port.out.CryptoPort;
import xavierroigmartin.v_integrity.application.port.out.NodeConfigurationPort;
import xavierroigmartin.v_integrity.domain.Block;
import xavierroigmartin.v_integrity.domain.EvidenceRecord;
import xavierroigmartin.v_integrity.interfaces.rest.dto.EvidenceRequest;
import xavierroigmartin.v_integrity.interfaces.rest.dto.VerifyRequest;

@RestController
@RequestMapping("/api")
public class LedgerController {

    private final LedgerService ledger;
    private final CryptoPort crypto;
    private final NodeConfigurationPort nodeConfig;

    public LedgerController(LedgerService ledger, CryptoPort crypto, NodeConfigurationPort nodeConfig) {
        this.ledger = ledger;
        this.crypto = crypto;
        this.nodeConfig = nodeConfig;
    }

    @GetMapping("/chain")
    public Map<String, Object> chain() {
        return Map.of("length", ledger.chain().size(), "chain", ledger.chain());
    }

    @GetMapping("/mempool")
    public Map<String, Object> mempool() {
        return Map.of("size", ledger.mempool().size(), "mempool", ledger.mempool());
    }

    @PostMapping("/evidences")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> submitEvidence(@Valid @RequestBody EvidenceRequest req) {
        EvidenceRecord evidence = new EvidenceRecord(
                null,
                req.homologationId(),
                req.testRunId(),
                req.artifactName(),
                req.artifactType(),
                req.hashAlgorithm(),
                req.hash(),
                req.sizeBytes(),
                req.createdBy(),
                req.storageUri(),
                req.standards(),
                null
        );
        EvidenceRecord stored = ledger.submitEvidence(evidence);
        return Map.of("evidence", stored);
    }

    @PostMapping("/blocks/commit")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> commit() {
        Block block = ledger.commitAsLeader();
        return Map.of("block", block);
    }

    @PostMapping("/blocks/receive")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> receive(@RequestBody Block incoming) {
        ledger.acceptReplicatedBlock(incoming);
        return Map.of("accepted", true, "height", incoming.height());
    }

    @GetMapping("/validate")
    public Map<String, Object> validate() {
        return Map.of("valid", ledger.isValidLocalChain());
    }

    /**
     * Consulta directa por hash (útil para debug / demos).
     */
    @GetMapping("/evidences/hash/{hash}")
    public Map<String, Object> getEvidenceByHash(@PathVariable String hash) {
        Optional<LedgerService.EvidenceProof> found = ledger.findEvidenceByHash(hash);
        return found.map(evidenceProof -> Map.of(
                "found", true,
                "evidence", evidenceProof.evidence(),
                "proof", proofFrom(evidenceProof)
        )).orElseGet(() -> Map.of("found", false));
    }

    /**
     * Verificación por hash: devuelve proof orientado a auditoría.
     */
    @PostMapping("/verify")
    public Map<String, Object> verify(@Valid @RequestBody VerifyRequest req) {
        String hash = req.hash().trim().toLowerCase(Locale.ROOT);
        Optional<LedgerService.EvidenceProof> found = ledger.findEvidenceByHash(hash);

        if (found.isEmpty()) {
            return Map.of(
                    "verified", false,
                    "reason", "NOT_FOUND",
                    "hash", hash
            );
        }

        Map<String, Object> proof = proofFrom(found.get());
        return Map.of(
                "verified", true,
                "hash", hash,
                "evidence", found.get().evidence(),
                "proof", proof
        );
    }

    private Map<String, Object> proofFrom(LedgerService.EvidenceProof ep) {
        Block b = ep.block();

        // Validación explícita de firma (para “proof” en demo)
        String pubKey = nodeConfig.getAllowedNodePublicKeys().get(b.proposerNodeId());
        boolean signatureValid = false;
        if (pubKey != null && !pubKey.isBlank() && !"GENESIS".equals(b.proposerNodeId())) {
            signatureValid = crypto.verifyEd25519(hexToBytes(b.hash()), b.signature(), pubKey);
        }

        return Map.of(
                "blockHeight", b.height(),
                "blockTimestamp", b.timestamp().toString(),
                "blockHash", b.hash(),
                "previousHash", b.previousHash(),
                "signedBy", b.proposerNodeId(),
                "signatureValid", signatureValid
        );
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
