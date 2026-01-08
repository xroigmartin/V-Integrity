package xavierroigmartin.v_integrity;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

  // Use default 'postgres' superuser for tests to avoid permission issues during schema creation
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
      .withDatabaseName("ledger")
      .withUsername("postgres") 
      .withPassword("postgres");

  @BeforeAll
  static void runMigrations() {
      // Manually run Flyway to ensure migrations are applied
      Flyway flyway = Flyway.configure()
          .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
          .schemas("ledger")
          .defaultSchema("ledger")
          .locations("classpath:db/migration")
          .load();
      
      flyway.migrate();
      System.out.println("=== MANUAL FLYWAY MIGRATION EXECUTED ===");
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    
    // Force dialect to avoid "Unable to determine Dialect" error during startup
    registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    
    // Ensure Hibernate uses the correct schema
    registry.add("spring.jpa.properties.hibernate.default_schema", () -> "ledger");
    
    // Disable Spring Boot's Flyway auto-configuration since we run it manually
    registry.add("spring.flyway.enabled", () -> "false");
    
    // Re-enable validation to verify everything is correct now
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }
}
