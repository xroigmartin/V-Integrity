package xavierroigmartin.v_integrity;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VIntegrityApplication {

	static void main(String[] args) {
		SpringApplication.run(VIntegrityApplication.class, args);
	}

	@PostConstruct
	public void init() {
		// Set JVM timezone to UTC
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

}
