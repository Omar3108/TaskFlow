package com.taskflow.card;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CardSpecificationsTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        "login       | login",
        "100%        | 100\\%",
        "sprint_2    | sprint\\_2",
        "a\\b        | a\\\\b",
        "%_%         | \\%\\_\\%",
    })
    void likeWildcardsAreEscaped(String input, String expected) {
        assertThat(CardSpecifications.escapeLike(input)).isEqualTo(expected);
    }
}
