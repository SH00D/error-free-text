package com.example.errorfreetext.util;

import com.example.errorfreetext.client.SpellError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CorrectionApplier {

    private static final Logger log = LoggerFactory.getLogger(CorrectionApplier.class);

    public String apply(String text, List<SpellError> errors) {
        List<SpellError> sorted = new ArrayList<>(errors);
        sorted.sort((a, b) -> Integer.compare(b.pos(), a.pos()));

        StringBuilder result = new StringBuilder(text);
        for (SpellError error : sorted) {
            if (error.s() == null || error.s().isEmpty()) {
                continue;
            }
            int start = error.pos();
            int end = start + error.len();
            if (start < 0 || end > result.length() || !result.substring(start, end).equals(error.word())) {
                log.warn("Пропуск исправления '{}' на позиции {}: текст не совпадает", error.word(), start);
                continue;
            }
            result.replace(start, end, error.s().get(0));
        }
        return result.toString();
    }
}
