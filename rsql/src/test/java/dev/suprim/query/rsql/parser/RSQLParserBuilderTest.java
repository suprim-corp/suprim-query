package dev.suprim.query.rsql.parser;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for RSQLParserBuilder.
 */
class RSQLParserBuilderTest {

    @Test
    void constructor_shouldInstantiate() {
        // Cover the implicit constructor
        RSQLParserBuilder builder = new RSQLParserBuilder();
        assertThat(builder).isNotNull();
    }

    @Test
    void newRSQLParser_shouldReturnNonNullParser() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThat(parser).isNotNull();
    }

    @Test
    void newRSQLParser_shouldParseStandardOperators() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        Node node = parser.parse("name==john");

        assertThat(node).isNotNull();
    }

    @Test
    void newRSQLParser_shouldParseCustomLikeOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("name=like=john"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseCustomIlikeOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("name=ilike=john"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseCustomStartWithOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("name=startWith=jo"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseCustomEndWithOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("name=endWith=hn"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseCustomIsNullOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("name=isnull=true"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseCustomNotNullOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("name=nn=true"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseCustomJsonbContainOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        // Use single-quoted JSON string to avoid RSQL parser issues with double quotes
        assertThatCode(() -> parser.parse("meta=jbc=somevalue"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseCustomJsonContainOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("meta=jc=somevalue"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseCustomJsonbEqualOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("meta=jbe=somevalue"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseCustomJsonbKeyExistsOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("meta=jbKeyExist=mykey"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseCustomJsonContainsInArrayOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("roles=jcInArray=admin"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseCustomNotLikeOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("name=notlike=test"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseCustomJsonbArrowOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("meta=jba=type::aws"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseAndConditions() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("name==john;age=gt=18"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseOrConditions() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("name==john,name==jane"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseInOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("status=in=(active,pending,closed)"))
                .doesNotThrowAnyException();
    }

    @Test
    void newRSQLParser_shouldParseNotInOperator() {
        RSQLParser parser = RSQLParserBuilder.newRSQLParser();

        assertThatCode(() -> parser.parse("status=out=(deleted,archived)"))
                .doesNotThrowAnyException();
    }
}
