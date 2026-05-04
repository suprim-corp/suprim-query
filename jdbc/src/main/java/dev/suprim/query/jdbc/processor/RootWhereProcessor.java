package dev.suprim.query.jdbc.processor;

import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.model.DbWhere;
import dev.suprim.query.model.context.ReadContext;
import dev.suprim.query.rsql.parser.RSQLParserBuilder;
import dev.suprim.query.rsql.visitor.BaseRSQLVisitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import static java.util.Objects.nonNull;

@Slf4j
@Order(8)
@RequiredArgsConstructor
public class RootWhereProcessor implements ReadProcessor {
    private final JdbcManager jdbcManager;

    @Override
    public void process(ReadContext readContext) throws DbException {
        String filter = readContext.getFilter();
        if (nonNull(filter) && !filter.isBlank()) {
            readContext.createParamMap();

            DbWhere dbWhere = new DbWhere(
                    readContext.getTableName(),
                    readContext.getRoot(),
                    readContext.getCols(),
                    readContext.getParamMap(),
                    "read",
                    readContext.getAllTables()
            );

            log.debug("-Creating root where condition -");

            Node rootNode;

            try {
                rootNode = RSQLParserBuilder.newRSQLParser().parse(readContext.getFilter());
            } catch (RSQLParserException e) {
                log.error(e.getMessage(), e);
                throw new DbException(DbErrorCode.INVALID_REQUEST);
            }

            String where = rootNode.accept(
                    new BaseRSQLVisitor(
                            dbWhere,
                            jdbcManager.getDialect(readContext.getDbId())
                    )
            );

            log.debug("Where - {}", where);
            log.debug("param map - {}", readContext.getParamMap());

            readContext.setRootWhere(where);
        }
    }
}
