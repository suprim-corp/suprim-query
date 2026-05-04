package dev.suprim.query.model;

import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public record JoinDetail(
		String schemaName,
		String table,
		String withTable,
		List<String> fields,
		List<String> on,
		String filter,
		JoinType joinType
) {
	public String getJoinType() {
		return isNull(joinType) ? JoinType.INNER.symbol() : joinType.symbol();
	}

	public boolean hasWith() {
		return nonNull(withTable) && !withTable.isBlank();
	}

	public boolean hasOn() {
		return nonNull(on) && !on.isEmpty();
	}

	public boolean hasFilter() {
		return nonNull(filter) && !filter.isBlank();
	}
}
