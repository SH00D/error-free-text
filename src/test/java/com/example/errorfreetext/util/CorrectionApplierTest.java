package com.example.errorfreetext.util;

import com.example.errorfreetext.client.SpellError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorrectionApplierTest {

    private final CorrectionApplier applier = new CorrectionApplier();

    @Test
    void returnsTextUnchangedWhenNoErrors() {
        assertThat(applier.apply("всё верно", List.of())).isEqualTo("всё верно");
    }

    @Test
    void replacesErrorsWithFirstSuggestionKeepingPositions() {
        String text = "превет как дила";
        List<SpellError> errors = List.of(
                error(0, "превет", List.of("привет", "превед")),
                error(11, "дила", List.of("дела")));

        assertThat(applier.apply(text, errors)).isEqualTo("привет как дела");
    }

    @Test
    void appliesErrorsRegardlessOfInputOrder() {
        String text = "aa bb cc";
        List<SpellError> errors = List.of(
                error(0, "aa", List.of("aaaa")),
                error(6, "cc", List.of("c")),
                error(3, "bb", List.of("bbbbbb")));

        assertThat(applier.apply(text, errors)).isEqualTo("aaaa bbbbbb c");
    }

    @Test
    void skipsErrorsWithoutSuggestions() {
        assertThat(applier.apply("абвгд текст", List.of(error(0, "абвгд", List.of()))))
                .isEqualTo("абвгд текст");
    }

    @Test
    void skipsErrorsWhenPositionDoesNotMatchWord() {
        assertThat(applier.apply("hello world", List.of(error(0, "wrld", List.of("world")))))
                .isEqualTo("hello world");
    }

    private static SpellError error(int pos, String word, List<String> suggestions) {
        return new SpellError(1, pos, 0, pos, word.length(), word, suggestions);
    }
}
