package xavierroigmartin.v_integrity.application.port.out;

public interface HashingPort {
    String sha256Hex(String input);
    byte[] sha256Bytes(String input);
}
