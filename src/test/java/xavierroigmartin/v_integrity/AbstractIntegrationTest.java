package xavierroigmartin.v_integrity;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

  // Use default 'postgres' superuser for tests to avoid permission issues during schema creation
  // Singleton Container Pattern: Static field initialized once
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
      .withDatabaseName("ledger")
      .withUsername("postgres") 
      .withPassword("postgres")
      .withStartupTimeout(Duration.ofMinutes(2)); // Increased timeout for CI environments

  static {
    try {
      postgres.start();
    } catch (Exception e) {
      System.err.println("FAILED TO START POSTGRES CONTAINER: " + e.getMessage());
      throw new RuntimeException("Could not start Testcontainers", e);
    }
  }

  @BeforeAll
  static void setupDatabase() {
      try {
        // 1. Clean Database (Truncate tables to ensure fresh state for each test class)
        // This prevents "RehydrationFailedException" when ApplicationContext reloads and finds dirty data from previous tests
        try (Connection conn = postgres.createConnection(""); Statement stmt = conn.createStatement()) {
            // We need to check if tables exist before truncating, or just ignore errors if they don't
            // Simplest way: Try to truncate, ignore if table doesn't exist (first run)
            try {
                stmt.execute("TRUNCATE TABLE ledger.block_evidences, ledger.evidences, ledger.blocks CASCADE");
                System.out.println("=== DATABASE TRUNCATED SUCCESSFULLY ===");
            } catch (Exception e) {
                // Ignore, tables might not exist yet
                System.out.println("=== DATABASE TRUNCATE SKIPPED (Tables likely missing) ===");
            }
        }

        // 2. Run Migrations
        Flyway flyway = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .schemas("ledger")
            .defaultSchema("ledger")
            .locations("classpath:db/migration")
            .load();
        
        flyway.migrate();
        System.out.println("=== MANUAL FLYWAY MIGRATION EXECUTED SUCCESSFULLY ===");
      } catch (Exception e) {
        System.err.println("=== DATABASE SETUP FAILED ===");
        e.printStackTrace();
        throw new RuntimeException("Database setup failed in test", e);
      }
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
