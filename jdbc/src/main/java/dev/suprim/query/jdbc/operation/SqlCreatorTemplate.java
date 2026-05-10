package dev.suprim.query.jdbc.operation;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbSort;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.context.CreateContext;
import dev.suprim.query.model.context.DeleteContext;
import dev.suprim.query.model.context.ReadContext;
import dev.suprim.query.model.context.UpdateContext;
import gg.jte.TemplateEngine;
import gg.jte.TemplateNotFoundException;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public record SqlCreatorTemplate(
        TemplateEngine templateEngine,
        JdbcManager jdbcManager
) {

    public String updateQuery(UpdateContext updateContext) throws DbException {
        Map<String, Object> params = new HashMap<>();
        DbTable table = updateContext.getTable();
        Dialect dialect = jdbcManager.getDialect(updateContext.getDbId());

        if (dialect.supportAlias()) {
            params.put("rootTable", table.render());
        } else {
            params.put("rootTable", table.name());
        }

        params.put("rootWhere", updateContext.getWhere());
        params.put("columnSets", updateContext.renderSetColumns());
        params.put("rootTableAlias", table.alias());

        return renderSqlTemplate(dialect.getUpdateSqlTemplate(), params);
    }

    public String deleteQuery(DeleteContext deleteContext) throws DbException {
        Dialect dialect = jdbcManager.getDialect(deleteContext.getDbId());

        String where = deleteContext.getWhere();
        String rendererTableName = dialect.renderTableName(
                deleteContext.getTable(),
                nonNull(where) && !where.isBlank(),
                true
        );

        log.info("rendererTableName - {}", rendererTableName);

        Map<String, Object> params = new HashMap<>();
        params.put("rootTable", rendererTableName);
        params.put("rootWhere", deleteContext.getWhere());
        params.put("rootTableAlias", deleteContext.getTable().alias());

        return this.renderSqlTemplate(dialect.getDeleteSqlTemplate(), params);
    }

    /**
     * Generates an UPDATE ... SET column = current_timestamp statement for soft-delete operations.
     */
    public String softDeleteQuery(DeleteContext deleteContext, String softDeleteColumn) throws DbException {
        Dialect dialect = jdbcManager.getDialect(deleteContext.getDbId());
        DbTable table = deleteContext.getTable();

        String rendererTableName;
        if (dialect.supportAlias()) {
            rendererTableName = table.render();
        } else {
            rendererTableName = table.name();
        }

        String alias = table.alias();
        String qualifiedColumn = (!isNull(alias) && !alias.isBlank())
                ? alias + "." + softDeleteColumn
                : softDeleteColumn;

        String columnSets = qualifiedColumn + " = " + dialect.currentTimestamp();

        Map<String, Object> params = new HashMap<>();
        params.put("rootTable", rendererTableName);
        params.put("rootWhere", deleteContext.getWhere());
        params.put("columnSets", columnSets);
        params.put("rootTableAlias", alias);

        return this.renderSqlTemplate(dialect.getUpdateSqlTemplate(), params);
    }

    public String create(CreateContext createContext) throws DbException {
        Map<String, Object> params = new HashMap<>();

        params.put("table", createContext.table().fullName());
        params.put("columns", createContext.renderColumns());
        params.put("parameters", createContext.renderParams());

        Dialect dialect = jdbcManager.getDialect(createContext.dbId());

        return this.renderSqlTemplate(dialect.getInsertSqlTemplate(), params);
    }

    public String upsert(CreateContext createContext, String onConflictClause) throws DbException {
        Map<String, Object> params = new HashMap<>();

        params.put("table", createContext.table().fullName());
        params.put("columns", createContext.renderColumns());
        params.put("parameters", createContext.renderParams());
        params.put("onConflict", onConflictClause);

        Dialect dialect = jdbcManager.getDialect(createContext.dbId());

        return this.renderSqlTemplate(dialect.getUpsertSqlTemplate(), params);
    }

    public String findOne(ReadContext readContext) throws DbException {
        Map<String, Object> params = new HashMap<>();
        params.put("columns", projections(readContext.getCols()));
        params.put("rootTable", readContext.getRoot().render());
        params.put("rootWhere", readContext.getRootWhere());

        Dialect dialect = jdbcManager.getDialect(readContext.getDbId());

        return this.renderSqlTemplate(dialect.getFindOneSqlTemplate(), params);
    }

    public String count(ReadContext readContext) throws DbException {
        Map<String, Object> params = new HashMap<>();

        params.put("rootTable", readContext.getRoot().render());
        params.put("rootWhere", readContext.getRootWhere());

        Dialect dialect = jdbcManager.getDialect(readContext.getDbId());

        return this.renderSqlTemplate(dialect.getCountSqlTemplate(), params);
    }

    public String exists(ReadContext readContext) throws DbException {
        Map<String, Object> params = new HashMap<>();

        params.put("rootTable", readContext.getRoot().render());
        params.put("rootWhere", readContext.getRootWhere());
        params.put("joins", readContext.getDbJoins());

        Dialect dialect = jdbcManager.getDialect(readContext.getDbId());

        return this.renderSqlTemplate(dialect.getExistSqlTemplate(), params);
    }

    public String query(ReadContext readContext) throws DbException {
        log.debug("**** Preparing to render ****");

        Map<String, Object> params = new HashMap<>();
        params.put("columns", projections(readContext.getCols()));
        params.put("rootTable", readContext.getRoot().render());
        params.put("rootWhere", readContext.getRootWhere());
        params.put("joins", readContext.getDbJoins());

        if (nonNull(readContext.getDbSortList()) && !readContext.getDbSortList().isEmpty()) {
            params.put("sorts", orderBy(readContext.getDbSortList()));
        }

        log.debug("limit - {}", readContext.getLimit());
        log.debug("offset - {}", readContext.getOffset());

        if (readContext.getLimit() > -1) {
            params.put("limit", readContext.getLimit());
        }

        if (readContext.getLimit() == -1) {
            params.put("limit", readContext.getDefaultFetchLimit());
        }

        if (readContext.getOffset() > -1) {
            params.put("offset", readContext.getOffset());
        }

        log.debug("data - {}", params);

        Dialect dialect = jdbcManager.getDialect(readContext.getDbId());

        return this.renderSqlTemplate(dialect.getReadSqlTemplate(), params);
    }

    private String renderSqlTemplate(
            String template,
            Map<String, Object> params
    ) throws DbException {
        TemplateOutput output = new StringOutput();

        try {
            templateEngine.render(template + ".jte", params, output);
        } catch (TemplateNotFoundException e) {
            log.error("Template not found - {}", template);
            throw new DbException(DbErrorCode.SERVER_ERROR);
        }

        return output.toString();
    }

    private String projections(List<DbColumn> columns) {
        List<String> columList = columns.stream()
                .map(DbColumn::renderWithAlias)
                .toList();

        return String.join("\n\t,", columList);
    }

    private String orderBy(List<DbSort> sorts) {
        List<String> sortList = sorts.stream()
                .map(DbSort::render)
                .toList();

        return String.join("\n\t,", sortList);
    }
}
