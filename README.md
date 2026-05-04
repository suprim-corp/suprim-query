# Suprim Query

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)
[![](https://jitpack.io/v/dev.suprim/suprim-query.svg)](https://jitpack.io/#dev.suprim/suprim-query)
[![codecov](https://codecov.io/gh/suprim-corp/suprim-query/branch/main/graph/badge.svg)](https://codecov.io/gh/suprim-corp/suprim-query)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Type-safe dynamic SQL query builder for Spring Boot applications. Supports RSQL filtering, JTE SQL templating,
multi-tenant routing, and automatic metadata extraction.

## Modules

| Module                | Description                                           |
|-----------------------|-------------------------------------------------------|
| `core`                | Core models, dialect interface, exceptions, utilities |
| `rsql`                | RSQL parser integration and fluent filter/join builders |
| `jdbc`                | Spring JDBC operations with JTE SQL templating        |
| `postgresql`          | PostgreSQL dialect and metadata extraction            |
| `spring-boot-starter` | Spring Boot auto-configuration                        |

## Requirements

- Java 21+
- Maven 3.9+

## Installation

Add the JitPack repository and dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>dev.suprim</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

If you only need the RSQL builder (no JDBC/Spring dependency):

```xml
<dependency>
    <groupId>dev.suprim</groupId>
    <artifactId>rsql</artifactId>
    <version>1.0.0</version>
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

### Reading data

```java
@Autowired
private ReadService readService;

// Simple query with RSQL filter
ReadContext context = ReadContext.builder()
        .dbId("main")
        .schemaName("public")
        .tableName("users")
        .fields("id,name,email,age")
        .filter("age=gt=18;status==active")
        .limit(20)
        .offset(0)
        .build();

List<Map<String, Object>> users = readService.findAll(context);

// Find one record
ReadContext singleContext = ReadContext.builder()
        .dbId("main")
        .tableName("users")
        .fields("*")
        .filter("id==123")
        .build();

Map<String, Object> user = readService.findOne(singleContext);

// Count
ReadContext countContext = ReadContext.builder()
        .dbId("main")
        .tableName("orders")
        .filter("status==pending")
        .build();

long pendingCount = readService.count(countContext);
```

### Creating records

```java
@Autowired
private CreationService creationService;

Map<String, Object> data = Map.of(
        "name", "John Doe",
        "email", "john@example.com",
        "age", 30
);

// Simple insert
CreationResponse response = creationService.execute(
        "main", "public", "users",
        null,   // columns (null = use data keys)
        data,
        false,  // tsIdEnabled
        null    // sequences
);

// Insert with TSID auto-generated primary key
CreationResponse withTsid = creationService.execute(
        "main", "public", "users",
        null, data,
        true,   // generates TSID for PK columns
        null
);

// Insert with specific columns and sequence
List<String> columns = List.of("name", "email");
List<String> sequences = List.of("order_number:orders_seq");

CreationResponse withSeq = creationService.execute(
        "main", "public", "orders",
        columns, data, false, sequences
);
```

### Updating records

```java
@Autowired
private UpdateService updateService;

Map<String, Object> updates = Map.of(
        "status", "verified",
        "verified_at", "2026-01-15T10:30:00"
);

// Update with RSQL filter (filter is required)
int rowsUpdated = updateService.patch(
        "main", "public", "users",
        updates,
        "id==123"
);

// Update multiple records
int bulkUpdated = updateService.patch(
        "main", "public", "orders",
        Map.of("status", "cancelled"),
        "status==pending;created_at=lt=2026-01-01"
);
```

### Deleting records

```java
@Autowired
private DeleteService deleteService;

// Delete with RSQL filter (filter is required)
int rowsDeleted = deleteService.delete(
        "main", "public", "sessions",
        "expired_at=lt=2026-01-01"
);
```

### Building RSQL filters programmatically

Use `FilterBuilder` instead of concatenating RSQL strings manually:

```java
import dev.suprim.query.rsql.builder.FilterBuilder;

// Simple AND filter
String filter = FilterBuilder.and()
        .eq("status", "active")
        .gte("age", "18")
        .build();
// Result: (status=='active' and age=ge='18')

// OR filter
String orFilter = FilterBuilder.or()
        .eq("role", "admin")
        .eq("role", "moderator")
        .build();
// Result: (role=='admin' or role=='moderator')

// Nested conditions
String nested = FilterBuilder.and()
        .eq("workspace_id", workspaceId)
        .or(FilterBuilder.or()
                .eq("visibility", "public")
                .and(FilterBuilder.and()
                        .eq("visibility", "private")
                        .eq("owner_id", currentUserId)
                )
        )
        .build();

// IN operator
String inFilter = FilterBuilder.and()
        .in("status", "active", "pending", "review")
        .neq("deleted", "true")
        .build();

// Pattern matching
String searchFilter = FilterBuilder.and()
        .ilike("name", "john")
        .startWith("email", "john")
        .isNotNull("verified_at")
        .build();

// JSONB operators (PostgreSQL)
String jsonFilter = FilterBuilder.and()
        .jsonbContains("metadata", "tier", "premium")
        .jsonbContains("config", Map.of("active", true, "plan", "annual"))
        .jsonbKeyExists("settings", "notifications")
        .build();

// Raw RSQL passthrough
String withRaw = FilterBuilder.and()
        .eq("type", "order")
        .raw("total=gt=100;total=lt=500")
        .build();
```

### Building JOINs

```java
import dev.suprim.query.rsql.builder.JoinBuilder;
import dev.suprim.query.rsql.builder.JoinBuilder.JoinCondition;
import dev.suprim.query.rsql.builder.JoinBuilder.JoinField;
import dev.suprim.query.model.JoinDetail;

// Inner join with specific fields
JoinDetail memberJoin = JoinBuilder.inner("workspace_members")
        .on(JoinCondition.eq("id", "workspace_id"))
        .fields(
                JoinField.of("member_id"),
                JoinField.aliased("role", "member_role")
        )
        .build();

// Left join with filter on joined table
JoinDetail orderJoin = JoinBuilder.left("orders")
        .on(JoinCondition.eq("id", "user_id"))
        .fields(JoinField.of("total"), JoinField.of("status"))
        .filter(FilterBuilder.and().eq("status", "completed"))
        .build();

// Multiple ON conditions
JoinDetail complexJoin = JoinBuilder.inner("permissions")
        .on(JoinCondition.eq("id", "resource_id"))
        .on(JoinCondition.of("type", JoinOperator.EQ, "resource_type"))
        .fields(JoinField.of("level"))
        .build();

// Use joins in a read context
ReadContext context = ReadContext.builder()
        .dbId("main")
        .tableName("users")
        .fields("*")
        .joins(List.of(memberJoin, orderJoin))
        .filter("status==active")
        .limit(50)
        .build();

List<Map<String, Object>> results = readService.findAll(context);
```

### Sorting

```java
// Sort by single column (default ASC)
ReadContext sorted = ReadContext.builder()
        .dbId("main")
        .tableName("users")
        .fields("*")
        .sorts(List.of("created_at;DESC"))
        .limit(10)
        .build();

// Multiple sort columns
ReadContext multiSort = ReadContext.builder()
        .dbId("main")
        .tableName("products")
        .fields("*")
        .sorts(List.of("category;ASC", "price;DESC", "name;ASC"))
        .limit(100)
        .build();
```

### Multi-tenant database routing

```java
import dev.suprim.query.jdbc.config.DatabaseContextHolder;

// Switch database context for the current thread
DatabaseContextHolder.setCurrentDbId("tenant_abc");

try {
    // All queries now route to tenant_abc's datasource
    List<Map<String, Object>> data = readService.findAll(
            ReadContext.builder()
                    .dbId("tenant_abc")
                    .tableName("invoices")
                    .fields("*")
                    .build()
    );
} finally {
    DatabaseContextHolder.clear();
}
```

## RSQL Operators

| Operator        | Description              | Example                          |
|-----------------|--------------------------|----------------------------------|
| `==`            | Equal                    | `status==active`                 |
| `!=`            | Not equal                | `role!=guest`                    |
| `=gt=`          | Greater than             | `age=gt=18`                      |
| `=ge=`          | Greater than or equal    | `price=ge=100`                   |
| `=lt=`          | Less than                | `stock=lt=5`                     |
| `=le=`          | Less than or equal       | `rating=le=3`                    |
| `=in=`          | In list                  | `status=in=(active,pending)`     |
| `=out=`         | Not in list              | `type=out=(draft,archived)`      |
| `=like=`        | LIKE pattern             | `name=like=john`                 |
| `=ilike=`       | Case-insensitive LIKE    | `email=ilike=JOHN`               |
| `=startWith=`   | Starts with              | `name=startWith=Jo`              |
| `=endWith=`     | Ends with                | `email=endWith=.com`             |
| `=isnull=`      | IS NULL                  | `deleted_at=isnull=true`         |
| `=nn=`          | IS NOT NULL              | `verified_at=nn=true`            |
| `=notlike=`     | NOT LIKE                 | `name=notlike=test`              |
| `=jbc=`         | JSONB contains (`@>`)    | `metadata=jbc={"key":"value"}`   |
| `=jbKeyExist=`  | JSONB key exists (`?`)   | `settings=jbKeyExist=theme`      |
| `=jba=`         | JSONB arrow (`->>`)      | `data.name=jba=John`             |

Logical operators: `;` (AND), `,` (OR). Use parentheses for grouping.

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
