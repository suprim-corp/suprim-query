package dev.suprim.query.autoconfigure;

import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * Converts Spring Data {@link Sort} to suprim-query sort format ({@code "field;ASC"} / {@code "field;DESC"}).
 */
public final class SortConverter {

	private SortConverter() {}

	/**
	 * Converts a Spring {@link Sort} to suprim-query sort list.
	 * Returns the provided default if sort is unsorted.
	 *
	 * @param sort         Spring sort
	 * @param defaultSort  fallback sort (e.g. {@code "created_at;DESC"})
	 * @return list of sort strings in suprim-query format
	 */
	public static List<String> from(Sort sort, String defaultSort) {
		if (sort == null || sort.isUnsorted()) {
			return List.of(defaultSort);
		}
		return sort.stream()
				.map(o -> o.getProperty() + ";" + (o.isAscending() ? "ASC" : "DESC"))
				.toList();
	}

	/**
	 * Converts a Spring {@link Sort} to suprim-query sort list.
	 * Returns empty list if sort is unsorted.
	 */
	public static List<String> from(Sort sort) {
		if (sort == null || sort.isUnsorted()) {
			return List.of();
		}
		return sort.stream()
				.map(o -> o.getProperty() + ";" + (o.isAscending() ? "ASC" : "DESC"))
				.toList();
	}
}
