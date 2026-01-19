package xavierroigmartin.v_integrity;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class VIntegrityApplicationIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void contextLoads() {
  }

  @Test
  void debugDatabaseSchema() {
    System.out.println("=== DEBUG DATABASE SCHEMA START ===");
    
    // Check schemas
    List<Map<String, Object>> schemas = jdbcTemplate.queryForList(
        "SELECT schema_name FROM information_schema.schemata"
    );
    System.out.println("SCHEMAS: " + schemas);

    // Check tables in 'ledger' schema
    List<Map<String, Object>> tables = jdbcTemplate.queryForList(
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'ledger'"
    );
    System.out.println("TABLES IN 'ledger': " + tables);
    
    // Check tables in 'public' schema (in case Flyway put them there)
    List<Map<String, Object>> publicTables = jdbcTemplate.queryForList(
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
    );
    System.out.println("TABLES IN 'public': " + publicTables);
    
    System.out.println("=== DEBUG DATABASE SCHEMA END ===");
  }

}
