package xavierroigmartin.v_integrity.infrastructure.adapter;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LogAdapterTest {

  private final LogAdapter logAdapter = new LogAdapter();

  @Test
  void should_log_business_event_without_error() {
    assertDoesNotThrow(() -> 
        logAdapter.logBusinessEvent("TEST_EVENT", Map.of("key", "value"))
    );
  }

  @Test
  void should_log_business_error_without_error() {
    assertDoesNotThrow(() -> 
        logAdapter.logBusinessError("TEST_ERROR", "Something went wrong", Map.of("context", "details"))
    );
  }
}
