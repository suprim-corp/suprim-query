package dev.suprim.query.jdbc.processor;

import cz.jirutka.rsql.parser.ast.Node;
import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.exception.DbRuntimeException;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.model.*;
import dev.suprim.query.model.context.ReadContext;
import dev.suprim.query.rsql.parser.RSQLParserBuilder;
import dev.suprim.query.rsql.visitor.BaseRSQLVisitor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.isNull;

import static dev.suprim.query.rsql.operators.OperatorMap.getRSQLOperator;
import static dev.suprim.query.rsql.operators.OperatorMap.getSQLOperator;
import static dev.suprim.query.util.AliasGenerator.getAlias;

@Slf4j
@Order(6)
@RequiredArgsConstructor
public class JoinProcessor implements ReadProcessor {
    private final JdbcManager jdbcManager;

    @Override
    public void process(ReadContext readContext) throws DbException {
        List<JoinDetail> joins = readContext.getJoins();

        if (isNull(joins) || joins.isEmpty()) {
            return;
        }

        DbTable rootTable = readContext.getRoot();

        List<DbTable> allJoinTables = new ArrayList<>();
        allJoinTables.add(rootTable);

        // Store the root table in ReadContext for cross-table column resolution
        readContext.addTable(rootTable);

        for (JoinDetail joinDetail : joins) {
            rootTable = reviewRootTable(
                    readContext.getDbId(),
                    allJoinTables,
                    joinDetail,
                    rootTable
            );

            String tableName = joinDetail.table();
            DbTable table = jdbcManager.getTable(
                    readContext.getDbId(),
                    readContext.getSchemaName(),
                    tableName
            );

            table = table.copyWithAlias(getAlias(tableName));

            List<DbColumn> columnList = addColumns(table, joinDetail.fields());
            readContext.addColumns(columnList);
            addJoin(table, rootTable, joinDetail, readContext);

            // Store joined table in ReadContext for cross-table column resolution
            readContext.addTable(table);

            allJoinTables.add(rootTable);

            rootTable = table;
        }
    }

    private DbTable reviewRootTable(
            String dbId,
            List<DbTable> allJoinTables,
            JoinDetail joinDetail,
            DbTable rootTable
    ) {
        if (allJoinTables.size() == 1) {
            return rootTable;
        }

        if (joinDetail.hasWith()) {
            // Check if existing table
            String withTable = joinDetail.withTable();
            Optional<DbTable> newRoot = allJoinTables.stream()
                    .filter(t -> withTable.equalsIgnoreCase(t.name()))
                    .findFirst();

            // Look in the cache
            return newRoot.orElseGet(() -> {
                try {
                    return jdbcManager.getTable(
                            dbId,
                            joinDetail.schemaName(),
                            withTable
                    );
                } catch (DbException e) {
                    throw new DbRuntimeException(e);
                }
            });
        }

        return rootTable;
    }

    private void addJoin(
            DbTable table,
            DbTable rootTable,
            JoinDetail joinDetail,
            ReadContext readContext
    ) throws DbException {
        DbJoin join = DbJoin.builder()
                .tableName(table.fullName())
                .alias(table.alias())
                .joinType(joinDetail.getJoinType())
                .build();

        addCondition(table, rootTable, joinDetail, join);

        processFilter(table, joinDetail, join, readContext);

        readContext.addJoin(join);
    }

    private void processFilter(
            DbTable table,
            JoinDetail joinDetail,
            DbJoin join,
            ReadContext readContext
    ) throws DbException {
        if (joinDetail.hasFilter()) {
            readContext.createParamMap();

            DbWhere dbWhere = new DbWhere(
                    table.name(),
                    table,
                    table.buildColumns(),
                    readContext.getParamMap(),
                    "read",
                    readContext.getAllTables()
            );

            Node rootNode = RSQLParserBuilder.newRSQLParser().parse(joinDetail.filter());

            Dialect dialect = jdbcManager.getDialect(readContext.getDbId());

            String where = rootNode.accept(
                    BaseRSQLVisitor.builder()
                            .dbWhere(dbWhere)
                            .dialect(dialect)
                            .build()
            );

            join.addAdditionalWhere(where);
        }
    }

    private void addCondition(
            DbTable table,
            DbTable rootTable,
            JoinDetail joinDetail,
            DbJoin dbJoin
    ) throws DbException {
        if (joinDetail.hasOn()) {
            int onIdx = 1;
            for (String on : joinDetail.on()) {
                processOn(on, onIdx, table, rootTable, dbJoin);
                onIdx++;
            }
        }
    }

    private void processOn(
            String onExpression,
            int onIdx,
            DbTable table,
            DbTable rootTable,
            DbJoin dbJoin
    ) throws DbException {
        String rSqlOperator = getRSQLOperator(onExpression);
        String operator = getSQLOperator(rSqlOperator);

        String left = onExpression.substring(0, onExpression.indexOf(rSqlOperator)).trim();
        String right = onExpression.substring(
                onExpression.indexOf(rSqlOperator) + rSqlOperator.length()
        ).trim();

        DbColumn leftColumn = rootTable.buildColumn(left);
        DbColumn rightColumn = table.buildColumn(right);

        if (onIdx == 1) {
            dbJoin.addOn(leftColumn, operator, rightColumn);
        } else {
            dbJoin.addAndCondition(leftColumn, operator, rightColumn);
        }
    }

    @SneakyThrows
    private List<DbColumn> addColumns(DbTable table, List<String> fields) {
        log.debug("Fields - {}", fields);

        List<DbColumn> columnList = new ArrayList<>();

        if (isNull(fields)) {
            // Include all fields of the root table
            columnList.addAll(table.buildColumns());
        } else {
            // Query has specific columns so parse and map it.
            List<DbColumn> columns = fields.stream()
                    .map((e) -> {
                        try {
                            return table.buildColumn(e);
                        } catch (DbException ex) {
                            log.error("Error building column - {}", e);
                            throw new DbRuntimeException(ex);
                        }
                    })
                    .toList();
            columnList.addAll(columns);
        }

        return columnList;
    }
}
