package xavierroigmartin.v_integrity.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        logger.info("Endpoint /hello called");
        logger.debug("This is a debug message showing the current time: {}", LocalDateTime.now());
        
        return Map.of(
            "message", "Hello from V-Integrity!",
            "timestamp", LocalDateTime.now()
        );
    }

    @GetMapping("/error")
    public void triggerError() {
        logger.warn("Endpoint /error called - triggering a runtime exception");
        throw new RuntimeException("This is a forced test exception");
    }
}
