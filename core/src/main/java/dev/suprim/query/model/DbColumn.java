package dev.suprim.query.model;

import java.util.Set;

import static java.util.Locale.ROOT;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public record DbColumn(
		String tableName, String name, String alias, String tableAlias,
		boolean pk, String columnDataTypeName, boolean generated,
		boolean autoIncremented, Class<?> typeMappedClass, String coverChar,
		String jsonParts
) {
	private String getQuotedName() {
		if (isNull(jsonParts) || jsonParts.isBlank()) {
			return coverChar + name + coverChar;
		}

		return coverChar + name + coverChar + jsonParts;
	}

	private String getQuotedAlias() {
		return coverChar + alias + coverChar;
	}

	public String render() {
		return tableAlias + "." + getQuotedName();
	}

	public String renderWithAlias() {
		String firstPart = tableAlias + "." + getQuotedName();
		if (nonNull(alias) && !alias.isBlank()) {
			return firstPart + " as " + getQuotedAlias();
		}
		return firstPart;
	}

	public String getAliasedName() {
		return tableAlias + "." + name;
	}

	public String getAliasedNameParam() {
		return tableAlias + "_" + name;
	}

	public boolean isDateTimeFamily() {
		return "timestamp".equalsIgnoreCase(columnDataTypeName);
	}

	public boolean isIntFamily() {
		return Set.of(
				"smallint",
				"bigint",
				"int8",
				"int4",
				"bigint unsigned",
				"integer",
				"number"
		).contains(columnDataTypeName.toLowerCase(ROOT));
	}

	public boolean isStringFamily() {
		return Set.of("varchar", "text", "varchar2")
		          .contains(columnDataTypeName.toLowerCase(ROOT));
	}

	public DbColumn copyWithAlias(DbAlias columnAlias) {
		return new DbColumn(
				tableName,
				name,
				columnAlias.alias(),
				tableAlias,
				pk,
				columnDataTypeName,
				generated,
				autoIncremented,
				typeMappedClass,
				coverChar,
				columnAlias.jsonParts()
		);
	}

	public DbColumn copyWithTableAlias(String tableAlias) {
		return new DbColumn(
				tableName,
				name,
				alias,
				tableAlias,
				pk,
				columnDataTypeName,
				generated,
				autoIncremented,
				typeMappedClass,
				coverChar,
				""
		);
	}
}
