# Contributing to V-Integrity

Thank you for your interest in contributing to V-Integrity! This document provides guidelines to ensure a smooth collaboration process.

## 🛠️ Development Setup

### Prerequisites
- **Java 25** (Eclipse Temurin recommended)
- **Docker** & **Docker Compose**
- **IntelliJ IDEA** (Recommended IDE)

### IDE Configuration (IntelliJ IDEA)
To ensure your code complies with the project's strict style guidelines (Google Java Style), please import the provided code style scheme:

1. Go to **Settings/Preferences** > **Editor** > **Code Style** > **Java**.
2. Click the **Gear Icon** (⚙️) next to the Scheme dropdown.
3. Select **Import Scheme** > **IntelliJ IDEA code style XML**.
4. Select the file located at `style/intellij-java-google-style.xml` in this repository.
5. Click **Apply**.

Now, `Ctrl+Alt+L` (Reformat Code) will automatically format your code according to the project rules.

### Build & Test
We use the Maven Wrapper to ensure consistency.

```bash
# Run unit tests
./mvnw test

# Run full verification (compilation, tests, checkstyle, jacoco)
./mvnw verify
```

## 📏 Coding Standards

- **Style**: We follow **Google Java Style**. The build will fail if Checkstyle rules are violated.
- **Architecture**: Strictly adhere to **Hexagonal Architecture** (Ports & Adapters).
  - Domain logic goes in `domain`.
  - Interfaces go in `application.port.out`.
  - Implementations go in `infrastructure.adapter`.
- **Documentation**: All public classes and interfaces must have JavaDoc in English.
- **Testing**: TDD is encouraged. New features must include unit tests.

## 🚀 Pull Request Process

1. Fork the repository and create your branch from `main`.
2. If you've added code that should be tested, add tests.
3. Ensure the test suite passes (`./mvnw verify`).
4. Make sure your code lints (`./mvnw checkstyle:check`).
5. Issue that pull request!

## 🔒 Security

- Never commit secrets or private keys.
- Use environment variables for sensitive configuration.
- Follow OWASP guidelines for API security.

Thank you for contributing!
