package com.example.errorfreetext.integration;

import com.example.errorfreetext.dto.CreateTaskResponse;
import com.example.errorfreetext.dto.TaskResponse;
import com.example.errorfreetext.entity.TaskStatus;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static WireMockServer speller = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        speller.start();
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.speller.url", speller::baseUrl);
        registry.add("app.scheduler.delay-ms", () -> "200");
    }

    @AfterAll
    static void stopSpeller() {
        speller.stop();
    }

    @Test
    void taskIsCorrectedByScheduler() {
        speller.stubFor(post(urlEqualTo("/checkTexts"))
                .withRequestBody(containing("helo"))
                .willReturn(okJson("""
                        [[{"code":1,"pos":0,"row":0,"col":0,"len":4,"word":"helo","s":["hello"]}]]
                        """)));

        CreateTaskResponse created = createTask("helo world");

        TaskResponse result = awaitFinalStatus(created);
        assertThat(result.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(result.correctedText()).isEqualTo("hello world");
    }

    @Test
    void taskGetsErrorStatusWhenSpellerFails() {
        speller.stubFor(post(urlEqualTo("/checkTexts"))
                .withRequestBody(containing("failing"))
                .willReturn(aResponse().withStatus(503)));

        CreateTaskResponse created = createTask("failing text");

        TaskResponse result = awaitFinalStatus(created);
        assertThat(result.status()).isEqualTo(TaskStatus.ERROR);
        assertThat(result.errorMessage()).startsWith("Ошибка запроса к API Яндекс.Спеллера");
    }

    private CreateTaskResponse createTask(String text) {
        ResponseEntity<CreateTaskResponse> response = restTemplate.postForEntity("/tasks",
                Map.of("text", text, "language", "EN"), CreateTaskResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private TaskResponse awaitFinalStatus(CreateTaskResponse created) {
        return await().atMost(Duration.ofSeconds(10))
                .until(() -> restTemplate.getForObject("/tasks/" + created.id(), TaskResponse.class),
                        task -> task.status() == TaskStatus.COMPLETED || task.status() == TaskStatus.ERROR);
    }
}
