package dev.suprim.query.autoconfigure;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SortConverterTest {

	@Nested
	class FromWithDefault {

		@Test
		void unsorted_shouldReturnDefault() {
			List<String> result = SortConverter.from(Sort.unsorted(), "created_at;DESC");
			assertThat(result).containsExactly("created_at;DESC");
		}

		@Test
		void null_shouldReturnDefault() {
			List<String> result = SortConverter.from(null, "created_at;DESC");
			assertThat(result).containsExactly("created_at;DESC");
		}

		@Test
		void singleAsc_shouldConvert() {
			Sort sort = Sort.by(Sort.Order.asc("name"));
			List<String> result = SortConverter.from(sort, "created_at;DESC");
			assertThat(result).containsExactly("name;ASC");
		}

		@Test
		void singleDesc_shouldConvert() {
			Sort sort = Sort.by(Sort.Order.desc("updated_at"));
			List<String> result = SortConverter.from(sort, "created_at;DESC");
			assertThat(result).containsExactly("updated_at;DESC");
		}

		@Test
		void multiple_shouldConvertAll() {
			Sort sort = Sort.by(Sort.Order.asc("name"), Sort.Order.desc("created_at"));
			List<String> result = SortConverter.from(sort, "id;ASC");
			assertThat(result).containsExactly("name;ASC", "created_at;DESC");
		}
	}

	@Nested
	class FromWithoutDefault {

		@Test
		void unsorted_shouldReturnEmpty() {
			List<String> result = SortConverter.from(Sort.unsorted());
			assertThat(result).isEmpty();
		}

		@Test
		void null_shouldReturnEmpty() {
			List<String> result = SortConverter.from(null);
			assertThat(result).isEmpty();
		}

		@Test
		void asc_shouldConvert() {
			Sort sort = Sort.by(Sort.Order.asc("name"));
			List<String> result = SortConverter.from(sort);
			assertThat(result).containsExactly("name;ASC");
		}

		@Test
		void desc_shouldConvert() {
			Sort sort = Sort.by(Sort.Order.desc("price"));
			List<String> result = SortConverter.from(sort);
			assertThat(result).containsExactly("price;DESC");
		}
	}
}
