package dev.suprim.query.rsql.visitor;

import cz.jirutka.rsql.parser.ast.*;
import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.exception.DbRuntimeException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;
import dev.suprim.query.rsql.handler.OperatorHandler;
import dev.suprim.query.rsql.handler.RSQLOperatorHandlers;
import dev.suprim.query.rsql.resolver.CrossTableColumnResolver;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
@Builder
public record BaseRSQLVisitor(
        DbWhere dbWhere,
        Dialect dialect
) implements RSQLVisitor<String, Object> {

    @Override
    public String visit(AndNode andNode, Object o) {
        return "( " + andNode.getChildren().stream()
                .map(node -> node.accept(this))
                .collect(Collectors.joining(" AND ")) + " ) ";
    }

    @Override
    public String visit(OrNode orNode, Object o) {
        return "( " +
               orNode.getChildren()
                       .stream()
                       .map(node -> node.accept(this))
                       .collect(Collectors.joining(" OR "))
               + " ) ";
    }

    @Override
    public String visit(ComparisonNode node, Object o) {
        ComparisonOperator op = node.getOperator();

        log.debug("Handling column - {}", node.getSelector());

        // Use CrossTableColumnResolver to handle cross-table column references
        DbColumn dbColumn;
        try {
            dbColumn = CrossTableColumnResolver.resolveColumn(
                    node.getSelector(),
                    this.dbWhere.allTables(),
                    this.dbWhere.table()
            );
        } catch (DbException e) {
            throw new DbRuntimeException(e);
        }

        Class<?> type = dbColumn.typeMappedClass();

        OperatorHandler operatorHandler = RSQLOperatorHandlers.getOperatorHandler(op.getSymbol());
        if (isNull(operatorHandler)) {
            throw new IllegalArgumentException(String.format(
                    "Operator '%s' is invalid",
                    op.getSymbol()
            ));
        }

        if (op.isMultiValue()) {
            try {
                return operatorHandler.handle(
                        dialect,
                        dbColumn,
                        this.dbWhere,
                        node.getArguments(),
                        type,
                        this.dbWhere.paramMap()
                );
            } catch (DbException e) {
                throw new DbRuntimeException(e);
            }
        } else {
            try {
                return operatorHandler.handle(
                        dialect,
                        dbColumn,
                        this.dbWhere,
                        node.getArguments().get(0),
                        type,
                        this.dbWhere.paramMap()
                );
            } catch (DbException e) {
                throw new DbRuntimeException(e);
            }
        }
    }
}
