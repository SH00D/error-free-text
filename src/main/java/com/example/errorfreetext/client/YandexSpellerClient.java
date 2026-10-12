package com.example.errorfreetext.client;

import com.example.errorfreetext.entity.Language;
import com.example.errorfreetext.exception.SpellerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class YandexSpellerClient {

    private static final Logger log = LoggerFactory.getLogger(YandexSpellerClient.class);

    private final RestTemplate restTemplate;
    private final String spellerUrl;

    public YandexSpellerClient(RestTemplate spellerRestTemplate,
                               @Value("${app.speller.url}") String spellerUrl) {
        this.restTemplate = spellerRestTemplate;
        this.spellerUrl = spellerUrl;
    }

    public List<List<SpellError>> checkTexts(List<String> texts, Language language, int options) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (String text : texts) {
            form.add("text", text);
        }
        form.add("lang", language.getCode());
        form.add("options", String.valueOf(options));
        form.add("format", "plain");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        SpellError[][] response;
        try {
            response = restTemplate.postForObject(spellerUrl + "/checkTexts", request, SpellError[][].class);
        } catch (RestClientException e) {
            log.error("Запрос к Яндекс.Спеллеру не удался: {}", e.getMessage());
            throw new SpellerException("Ошибка запроса к API Яндекс.Спеллера: " + e.getMessage(), e);
        }

        if (response == null || response.length != texts.size()) {
            throw new SpellerException("API Яндекс.Спеллера вернул неожиданный ответ");
        }

        List<List<SpellError>> result = new ArrayList<>();
        for (SpellError[] errors : response) {
            result.add(errors == null ? List.of() : List.of(errors));
        }
        return result;
    }
}
