# Stage 1: Build the application
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Copy maven wrapper and pom.xml first to cache dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline

# Copy source code and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port
EXPOSE 8081

# Environment variables (empty by default for security)
ENV LEDGER_NODE_ID=""
ENV LEDGER_LEADER=""
ENV LEDGER_PEERS=""
ENV LEDGER_PRIVATE_KEY_BASE64=""
ENV LEDGER_NODE1_PUBLIC_KEY_BASE64=""

# Entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]
