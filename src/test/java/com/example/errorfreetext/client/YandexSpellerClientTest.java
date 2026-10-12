package com.example.errorfreetext.client;

import com.example.errorfreetext.entity.Language;
import com.example.errorfreetext.exception.SpellerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YandexSpellerClientTest {

    private static final String BASE_URL = "http://speller.test";

    private MockRestServiceServer server;
    private YandexSpellerClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new YandexSpellerClient(restTemplate, BASE_URL);
    }

    @Test
    void sendsFormAndParsesErrors() {
        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("text", "превет");
        expectedForm.add("lang", "ru");
        expectedForm.add("options", "6");
        expectedForm.add("format", "plain");
        server.expect(requestTo(BASE_URL + "/checkTexts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(expectedForm))
                .andRespond(withSuccess("""
                        [[{"code":1,"pos":0,"row":0,"col":0,"len":6,"word":"превет","s":["привет"]}]]
                        """, MediaType.APPLICATION_JSON));

        List<List<SpellError>> result = client.checkTexts(List.of("превет"), Language.RU, 6);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsExactly(new SpellError(1, 0, 0, 0, 6, "превет", List.of("привет")));
        server.verify();
    }

    @Test
    void returnsEmptyListWhenNoErrors() {
        server.expect(requestTo(BASE_URL + "/checkTexts"))
                .andRespond(withSuccess("[[]]", MediaType.APPLICATION_JSON));

        assertThat(client.checkTexts(List.of("hello"), Language.EN, 0)).containsExactly(List.of());
    }

    @Test
    void throwsSpellerExceptionOnServerError() {
        server.expect(requestTo(BASE_URL + "/checkTexts")).andRespond(withServerError());

        assertThatThrownBy(() -> client.checkTexts(List.of("hello"), Language.EN, 0))
                .isInstanceOf(SpellerException.class)
                .hasMessageStartingWith("Ошибка запроса к API Яндекс.Спеллера");
    }

    @Test
    void throwsSpellerExceptionOnUnexpectedResponse() {
        server.expect(requestTo(BASE_URL + "/checkTexts"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.checkTexts(List.of("hello"), Language.EN, 0))
                .isInstanceOf(SpellerException.class)
                .hasMessage("API Яндекс.Спеллера вернул неожиданный ответ");
    }
}
