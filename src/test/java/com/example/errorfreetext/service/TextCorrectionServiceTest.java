package com.example.errorfreetext.service;

import com.example.errorfreetext.client.SpellError;
import com.example.errorfreetext.client.YandexSpellerClient;
import com.example.errorfreetext.entity.Language;
import com.example.errorfreetext.exception.SpellerException;
import com.example.errorfreetext.util.CorrectionApplier;
import com.example.errorfreetext.util.SpellerOptionsResolver;
import com.example.errorfreetext.util.TextSplitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextCorrectionServiceTest {

    @Mock
    private YandexSpellerClient spellerClient;

    private TextCorrectionService service;

    @BeforeEach
    void setUp() {
        service = createService(10_000);
    }

    @Test
    void correctsShortTextWithSingleRequest() {
        when(spellerClient.checkTexts(List.of("превет мир"), Language.RU, 0))
                .thenReturn(List.of(List.of(new SpellError(1, 0, 0, 0, 6, "превет", List.of("привет")))));

        assertThat(service.correct("превет мир", Language.RU)).isEqualTo("привет мир");
    }

    @Test
    void passesResolvedOptionsToApi() {
        String text = "visit https://example.com 2 times";
        when(spellerClient.checkTexts(List.of(text), Language.EN, 6)).thenReturn(List.of(List.of()));

        assertThat(service.correct(text, Language.EN)).isEqualTo(text);
    }

    @Test
    void longTextIsCorrectedInChunks() {
        service = createService(6);
        when(spellerClient.checkTexts(eq(List.of("helo ")), eq(Language.EN), anyInt()))
                .thenReturn(List.of(List.of(new SpellError(1, 0, 0, 0, 4, "helo", List.of("hello")))));
        when(spellerClient.checkTexts(eq(List.of("wrld")), eq(Language.EN), anyInt()))
                .thenReturn(List.of(List.of(new SpellError(1, 0, 0, 0, 4, "wrld", List.of("world")))));

        String corrected = service.correct("helo wrld", Language.EN);

        assertThat(corrected).isEqualTo("hello world");
    }

    @Test
    void textLongerThanLimitIsSentInSeveralRequests() {
        service = createService(10_000);
        String text = "слово ".repeat(4_000);
        when(spellerClient.checkTexts(anyList(), eq(Language.RU), anyInt())).thenReturn(List.of(List.of()));

        assertThat(service.correct(text, Language.RU)).isEqualTo(text);
        verify(spellerClient, times(3)).checkTexts(anyList(), eq(Language.RU), anyInt());
    }

    @Test
    void propagatesSpellerException() {
        when(spellerClient.checkTexts(anyList(), eq(Language.RU), anyInt()))
                .thenThrow(new SpellerException("API недоступен"));

        assertThatThrownBy(() -> service.correct("какой-то текст", Language.RU))
                .isInstanceOf(SpellerException.class)
                .hasMessage("API недоступен");
    }

    private TextCorrectionService createService(int maxTextLength) {
        return new TextCorrectionService(spellerClient, new TextSplitter(), new SpellerOptionsResolver(),
                new CorrectionApplier(), maxTextLength);
    }
}
