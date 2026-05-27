package dev.suprim.query.model.context;

import dev.suprim.query.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static java.util.Objects.isNull;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Slf4j
public class ReadContext {
    // Input Attributes
    String dbId;
    String schemaName;
    String tableName;
    String fields;
    String filter;
    List<String> sorts;
    int limit;
    long offset;
    List<JoinDetail> joins;
    int defaultFetchLimit;
    boolean includeSoftDeleted;
    @Builder.Default
    List<ExpressionField> expressions = List.of();
    /** Qualified column references for GROUP BY, e.g. {@code u."name"}. Must match SELECT projection. */
    @Builder.Default
    List<String> groupBys = List.of();

    // Derived attributes
    DbTable root;
    List<DbColumn> cols;
    String rootWhere;
    Map<String, Object> paramMap;
    List<DbJoin> dbJoins;
    List<DbSort> dbSortList;
    List<DbTable> allTables;

    public void createParamMap() {
        if (isNull(paramMap)) {
            paramMap = new HashMap<>();
        }
    }

    public void addColumns(List<DbColumn> columnList) {
        if (isNull(cols)) {
            cols = new ArrayList<>();
        }
        this.cols.addAll(columnList);
    }

    public void addJoin(DbJoin join) {
        if (isNull(dbJoins)) {
            dbJoins = new ArrayList<>();
        }
        dbJoins.add(join);
    }

    public void addTable(DbTable table) {
        if (isNull(allTables)) {
            allTables = new ArrayList<>();
        }
        allTables.add(table);
    }
}
