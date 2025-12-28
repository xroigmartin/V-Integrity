package xavierroigmartin.v_integrity.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MdcFilterTest {

  @Mock
  private NodeProperties nodeProperties;
  @Mock
  private FilterChain filterChain;
  @Mock
  private ServletRequest request;
  @Mock
  private ServletResponse response;

  @Test
  void should_add_node_id_to_mdc_during_request() throws Exception {
    // Given
    when(nodeProperties.getNodeId()).thenReturn("test-node");
    MdcFilter filter = new MdcFilter(nodeProperties);

    // When
    filter.doFilter(request, response, (req, res) -> {
      // Verify inside the chain execution
      assertEquals("test-node", MDC.get("node_id"));
    });

    // Then
    // Verify MDC is cleared after execution
    assertNull(MDC.get("node_id"));
  }

  @Test
  void should_clear_mdc_even_if_chain_throws_exception() throws Exception {
    // Given
    when(nodeProperties.getNodeId()).thenReturn("test-node");
    doThrow(new RuntimeException("Oops")).when(filterChain).doFilter(any(), any());
    
    MdcFilter filter = new MdcFilter(nodeProperties);

    // When/Then
    try {
      filter.doFilter(request, response, filterChain);
    } catch (RuntimeException e) {
      // Expected
    }

    // Verify MDC is cleared
    assertNull(MDC.get("node_id"));
  }
}
