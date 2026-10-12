package com.example.errorfreetext.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextSplitterTest {

    private final TextSplitter splitter = new TextSplitter();

    @Test
    void shortTextIsReturnedAsSingleChunk() {
        assertThat(splitter.split("hello world", 100)).containsExactly("hello world");
    }

    @Test
    void textOfExactlyMaxLengthIsNotSplit() {
        String text = "a".repeat(10_000);

        assertThat(splitter.split(text, 10_000)).containsExactly(text);
    }

    @Test
    void longTextIsSplitOnWhitespaceWithoutBreakingWords() {
        List<String> chunks = splitter.split("one two three four", 9);

        assertThat(chunks).containsExactly("one two ", "three ", "four");
    }

    @Test
    void chunksAreNotLongerThanLimitAndRestoreOriginalText() {
        String text = "word ".repeat(5_000) + "end";

        List<String> chunks = splitter.split(text, 10_000);

        assertThat(chunks).hasSizeGreaterThan(2);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(10_000));
        assertThat(String.join("", chunks)).isEqualTo(text);
    }

    @Test
    void wordLongerThanLimitIsCutHard() {
        assertThat(splitter.split("abcdefghij", 4)).containsExactly("abcd", "efgh", "ij");
    }

    @Test
    void nonPositiveLimitIsRejected() {
        assertThatThrownBy(() -> splitter.split("text", 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
