# Suprim Query

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Multi-module Java library for database query operations with RSQL filtering, JTE SQL templating, and Spring Boot
auto-configuration.

## Modules

| Module                   | Description                                           |
|--------------------------|-------------------------------------------------------|
| `db-core`                | Core models, dialect interface, exceptions, utilities |
| `db-rsql`                | RSQL parser integration for dynamic SQL filtering     |
| `db-jdbc`                | Spring JDBC operations with JTE templating            |
| `db-postgresql`          | PostgreSQL dialect and metadata extraction            |
| `db-spring-boot-starter` | Spring Boot auto-configuration                        |

## Requirements

- Java 21+
- Maven 3.9+
- Docker (for tests)

## Installation

Add to your `pom.xml`:

```xml

<dependency>
	<groupId>dev.suprim</groupId>
	<artifactId>db-spring-boot-starter</artifactId>
	<version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration

```yaml
db:
    enabled: true
    default-database-id: main
    databases:
        -   id: main
            type: postgresql
            url: jdbc:postgresql://localhost:5432/mydb
            username: user
            password: pass
            max-connections: 10
            schemas:
                - public
```

## Usage

```java

@Autowired
private ReadService readService;

@Autowired
private CreationService creationService;

// Read with RSQL filter
var request = ReadRequest.builder()
                         .schemaName("public")
                         .tableName("users")
                         .filter("age>=18;status==active")
                         .fields("id,name,email")
                         .limit(10)
                         .build();

List<Map<String, Object>> results = readService.read(request);

// Create
var data = Map.of("name", "John", "email", "john@example.com");
var created = creationService.create("public", "users", data);
```

## RSQL Operators

| Operator   | Description           |
|------------|-----------------------|
| `==`       | Equal                 |
| `!=`       | Not equal             |
| `=gt=`     | Greater than          |
| `=ge=`     | Greater than or equal |
| `=lt=`     | Less than             |
| `=le=`     | Less than or equal    |
| `=in=`     | In list               |
| `=out=`    | Not in list           |
| `=like=`   | LIKE pattern          |
| `=ilike=`  | Case-insensitive LIKE |
| `=isnull=` | IS NULL check         |

## Build

```bash
# Compile
mvn clean compile

# Test
mvn test

# Install locally
mvn clean install

# Package with coverage
mvn clean verify
```

Coverage reports generated at:

- Per-module: `{module}/target/site/jacoco/index.html`
- Aggregate: `target/site/jacoco-aggregate/index.html`

## License

MIT - See [LICENSE](LICENSE) for details.
