# Suprim Query

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)
[![](https://jitpack.io/v/dev.suprim/suprim-query.svg)](https://jitpack.io/#dev.suprim/suprim-query)
[![codecov](https://codecov.io/gh/suprim-corp/suprim-query/branch/main/graph/badge.svg)](https://codecov.io/gh/suprim-corp/suprim-query)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Type-safe dynamic SQL query builder for Spring Boot applications. Supports RSQL filtering, JTE SQL templating,
multi-tenant routing, and automatic metadata extraction.

## Modules

| Module                | Description                                             |
|-----------------------|---------------------------------------------------------|
| `core`                | Core models, dialect interface, exceptions, utilities   |
| `rsql`                | RSQL parser integration and fluent filter/join builders |
| `jdbc`                | Spring JDBC operations with JTE SQL templating          |
| `postgresql`          | PostgreSQL dialect and metadata extraction              |
| `spring-boot-starter` | Spring Boot auto-configuration                          |

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

// Paginated query (single call, returns data + metadata)
ReadContext pageContext = ReadContext.builder()
                                     .dbId("main")
                                     .tableName("users")
                                     .fields("id,name,email")
                                     .filter("status==active")
                                     .limit(20)
                                     .offset(40)
                                     .build();

Page page = readService.findPage(pageContext);
// page.data()    → List<Map<String, Object>> (current page rows)
// page.total()   → 150 (total matching rows)
// page.limit()   → 20
// page.offset()  → 40
// page.hasNext() → true (40 + 20 < 150)
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

#### Bulk update (single transaction)

```java
import dev.suprim.query.model.dto.BulkUpdate;

// Each BulkUpdate has its own data and filter — all execute in one transaction
List<BulkUpdate> updates = List.of(
		new BulkUpdate(Map.of("status", "shipped"), "id==101"),
		new BulkUpdate(Map.of("status", "cancelled"), "id==102"),
		new BulkUpdate(Map.of("status", "refunded", "refunded_at", "2026-05-05"), "id==103")
);

int totalUpdated = updateService.patchBulk("main", "public", "orders", updates);
// If any single update fails, all changes are rolled back
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

#### Bulk delete (single transaction)

```java
// Multiple filters — each scopes a separate DELETE, all in one transaction
List<String> filters = List.of(
		"status==expired;created_at=lt=2025-01-01",
		"status==cancelled;updated_at=lt=2025-06-01",
		"id==999"
);

int totalDeleted = deleteService.deleteBulk("main", "public", "sessions", filters);
// If any single delete fails, all changes are rolled back
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

### Filter → SQL mapping

Shows what SQL each RSQL filter generates (assuming table `users` with alias `t0`):

| FilterBuilder code                              | RSQL output                                  | Generated SQL WHERE                         |
|-------------------------------------------------|----------------------------------------------|---------------------------------------------|
| `.eq("status", "active")`                       | `status=='active'`                           | `t0."status" = :status`                     |
| `.neq("role", "guest")`                         | `role!='guest'`                              | `t0."role" <> :role`                        |
| `.gt("age", "18")`                              | `age=gt='18'`                                | `t0."age" > :age`                           |
| `.gte("price", "100")`                          | `price=ge='100'`                             | `t0."price" >= :price`                      |
| `.lt("stock", "5")`                             | `stock=lt='5'`                               | `t0."stock" < :stock`                       |
| `.lte("rating", "3")`                           | `rating=le='3'`                              | `t0."rating" <= :rating`                    |
| `.in("status", "active", "pending")`            | `status=in=(active,pending)`                 | `t0."status" IN (:status)`                  |
| `.notIn("type", "draft", "archived")`           | `type=out=(draft,archived)`                  | `t0."type" NOT IN (:type)`                  |
| `.like("name", "john")`                         | `name=like='john'`                           | `t0."name" LIKE :name` (value: `%john%`)    |
| `.ilike("email", "JOHN")`                       | `email=ilike='JOHN'`                         | `t0."email" ILIKE :email` (value: `%JOHN%`) |
| `.startWith("name", "Jo")`                      | `name=startWith='Jo'`                        | `t0."name" LIKE :name` (value: `Jo%`)       |
| `.endWith("email", ".com")`                     | `email=endWith='.com'`                       | `t0."email" LIKE :email` (value: `%.com`)   |
| `.isNull("deleted_at")`                         | `deleted_at=isnull='true'`                   | `t0."deleted_at" IS NULL`                   |
| `.isNotNull("verified_at")`                     | `verified_at=nn='true'`                      | `t0."verified_at" IS NOT NULL`              |
| `.jsonbContains("metadata", "tier", "premium")` | `metadata=jsonbContain='{"tier":"premium"}'` | `t0."metadata" @> :metadata::jsonb`         |
| `.jsonbKeyExists("settings", "theme")`          | `settings=jbKeyExist='theme'`                | `t0."settings" ? :settings`                 |

**Compound filters:**

```
FilterBuilder.and()
    .eq("status", "active")
    .gte("age", "18")
    .build()
```

RSQL: `(status=='active' and age=ge='18')`
SQL: `WHERE t0."status" = :status AND t0."age" >= :age`

```
FilterBuilder.or()
    .eq("role", "admin")
    .eq("role", "moderator")
    .build()
```

RSQL: `(role=='admin' or role=='moderator')`
SQL: `WHERE (t0."role" = :role OR t0."role" = :role_1)`

```
FilterBuilder.and()
    .eq("workspace_id", "ws-123")
    .or(FilterBuilder.or()
        .eq("visibility", "public")
        .and(FilterBuilder.and()
            .eq("visibility", "private")
            .eq("owner_id", "user-456")
        )
    )
    .build()
```

RSQL: `(workspace_id=='ws-123' and (visibility=='public' or (visibility=='private' and owner_id=='user-456')))`
SQL:

```sql
WHERE t0."workspace_id" = :workspace_id
  AND (t0."visibility" = :visibility OR (t0."visibility" = :visibility_1 AND t0."owner_id" = :owner_id))
```

**JOIN filter example:**

```
JoinBuilder.left("orders")
    .on(JoinCondition.eq("id", "user_id"))
    .fields(JoinField.of("total"))
    .filter(FilterBuilder.and().eq("status", "completed"))
    .build()
```

SQL:

```sql
LEFT JOIN "public"."orders" t1 ON t0."id" = t1."user_id" AND t1."status" = :status
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

try{
// All queries now route to tenant_abc's datasource
List<Map<String, Object>> data = readService.findAll(
		ReadContext.builder()
		           .dbId("tenant_abc")
		           .tableName("invoices")
		           .fields("*")
		           .build()
);
}finally{
		DatabaseContextHolder.

clear();
}
```

## RSQL Operators

| Operator       | Description            | Example                        |
|----------------|------------------------|--------------------------------|
| `==`           | Equal                  | `status==active`               |
| `!=`           | Not equal              | `role!=guest`                  |
| `=gt=`         | Greater than           | `age=gt=18`                    |
| `=ge=`         | Greater than or equal  | `price=ge=100`                 |
| `=lt=`         | Less than              | `stock=lt=5`                   |
| `=le=`         | Less than or equal     | `rating=le=3`                  |
| `=in=`         | In list                | `status=in=(active,pending)`   |
| `=out=`        | Not in list            | `type=out=(draft,archived)`    |
| `=like=`       | LIKE pattern           | `name=like=john`               |
| `=ilike=`      | Case-insensitive LIKE  | `email=ilike=JOHN`             |
| `=startWith=`  | Starts with            | `name=startWith=Jo`            |
| `=endWith=`    | Ends with              | `email=endWith=.com`           |
| `=isnull=`     | IS NULL                | `deleted_at=isnull=true`       |
| `=nn=`         | IS NOT NULL            | `verified_at=nn=true`          |
| `=notlike=`    | NOT LIKE               | `name=notlike=test`            |
| `=jbc=`        | JSONB contains (`@>`)  | `metadata=jbc={"key":"value"}` |
| `=jbKeyExist=` | JSONB key exists (`?`) | `settings=jbKeyExist=theme`    |
| `=jba=`        | JSONB arrow (`->>`)    | `data.name=jba=John`           |

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
