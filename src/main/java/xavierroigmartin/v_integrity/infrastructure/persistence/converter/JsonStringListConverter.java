package xavierroigmartin.v_integrity.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA Converter to map List<String> to JSONB column.
 */
@Converter
public class JsonStringListConverter implements AttributeConverter<List<String>, String> {

  private static final Logger logger = LoggerFactory.getLogger(JsonStringListConverter.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    if (attribute == null) {
      return "[]";
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      logger.error("Error converting list to JSON", e);
      return "[]";
    }
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isEmpty()) {
      return Collections.emptyList();
    }
    try {
      return objectMapper.readValue(dbData, new TypeReference<List<String>>() {});
    } catch (IOException e) {
      logger.error("Error converting JSON to list", e);
      return Collections.emptyList();
    }
  }
}
