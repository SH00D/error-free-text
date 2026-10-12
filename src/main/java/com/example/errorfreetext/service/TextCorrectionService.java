package com.example.errorfreetext.service;

import com.example.errorfreetext.client.SpellError;
import com.example.errorfreetext.client.YandexSpellerClient;
import com.example.errorfreetext.entity.Language;
import com.example.errorfreetext.util.CorrectionApplier;
import com.example.errorfreetext.util.SpellerOptionsResolver;
import com.example.errorfreetext.util.TextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TextCorrectionService {

    private final YandexSpellerClient spellerClient;
    private final TextSplitter textSplitter;
    private final SpellerOptionsResolver optionsResolver;
    private final CorrectionApplier correctionApplier;
    private final int maxTextLength;

    public TextCorrectionService(YandexSpellerClient spellerClient,
                                 TextSplitter textSplitter,
                                 SpellerOptionsResolver optionsResolver,
                                 CorrectionApplier correctionApplier,
                                 @Value("${app.speller.max-text-length}") int maxTextLength) {
        this.spellerClient = spellerClient;
        this.textSplitter = textSplitter;
        this.optionsResolver = optionsResolver;
        this.correctionApplier = correctionApplier;
        this.maxTextLength = maxTextLength;
    }

    public String correct(String text, Language language) {
        int options = optionsResolver.resolve(text);
        List<String> chunks = textSplitter.split(text, maxTextLength);

        StringBuilder corrected = new StringBuilder(text.length());
        for (String chunk : chunks) {
            List<SpellError> errors = spellerClient.checkTexts(List.of(chunk), language, options).get(0);
            corrected.append(correctionApplier.apply(chunk, errors));
        }
        return corrected.toString();
    }
}
