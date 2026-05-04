package dev.suprim.query.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AliasGeneratorTest {

    @Test
    void constructor_shouldInstantiate() {
        // Cover the implicit constructor
        AliasGenerator generator = new AliasGenerator();
        assertThat(generator).isNotNull();
    }

    @Test
    void getAlias_withLongIdentifier_shouldTruncateAndAppendUUID() {
        String alias = AliasGenerator.getAlias("user_accounts");

        assertThat(alias).startsWith("user_");
        assertThat(alias).hasSize(4 + 1 + 12); // 4 chars + underscore + 12 char UUID
    }

    @Test
    void getAlias_withShortIdentifier_shouldKeepFullNameAndAppendUUID() {
        String alias = AliasGenerator.getAlias("id");

        assertThat(alias).startsWith("id_");
        assertThat(alias).hasSize(2 + 1 + 12); // 2 chars + underscore + 12 char UUID
    }

    @Test
    void getAlias_withExactlyFourChars_shouldKeepFullNameAndAppendUUID() {
        String alias = AliasGenerator.getAlias("user");

        assertThat(alias).startsWith("user_");
        assertThat(alias).hasSize(4 + 1 + 12);
    }

    @Test
    void getAlias_shouldGenerateUniqueAliases() {
        String alias1 = AliasGenerator.getAlias("table");
        String alias2 = AliasGenerator.getAlias("table");

        assertThat(alias1).isNotEqualTo(alias2);
    }

    @Test
    void getAlias_withSingleChar_shouldAppendUUID() {
        String alias = AliasGenerator.getAlias("a");

        assertThat(alias).startsWith("a_");
        assertThat(alias).hasSize(1 + 1 + 12);
    }

    @Test
    void getAlias_withFiveChars_shouldTruncateToFour() {
        String alias = AliasGenerator.getAlias("users");

        assertThat(alias).startsWith("user_");
        assertThat(alias).hasSize(4 + 1 + 12);
    }

    @Test
    void getAlias_withEmptyString_shouldAppendUUID() {
        String alias = AliasGenerator.getAlias("");

        assertThat(alias).startsWith("_");
    }
}
