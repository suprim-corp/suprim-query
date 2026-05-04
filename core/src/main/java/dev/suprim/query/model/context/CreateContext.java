package dev.suprim.query.model.context;

import dev.suprim.query.model.DbTable;

import java.util.List;

import static java.util.Objects.isNull;

public record CreateContext(
        String dbId,
        DbTable table,
        List<String> insertableColumns,
        List<InsertableColumn> insertableColumnList
) {
    private List<String> getColumnNames() {
        return insertableColumnList.stream()
                .map(InsertableColumn::getColumnName)
                .toList();
    }

    private List<String> getParamNames() {
        return insertableColumnList.stream()
                .map(col -> {
                    String seq = col.getSequence();
                    return isNull(seq) || seq.isBlank() ? ":" + col.getColumnName() : seq;
                })
                .toList();
    }

    public String renderColumns() {
        return String.join(",", getColumnNames());
    }

    public String renderParams() {
        return String.join(",", getParamNames());
    }
}
