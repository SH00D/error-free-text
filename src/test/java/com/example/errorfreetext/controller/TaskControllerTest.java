package com.example.errorfreetext.controller;

import com.example.errorfreetext.dto.CreateTaskRequest;
import com.example.errorfreetext.dto.TaskResponse;
import com.example.errorfreetext.entity.Language;
import com.example.errorfreetext.entity.TaskStatus;
import com.example.errorfreetext.exception.TaskNotFoundException;
import com.example.errorfreetext.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void createTaskReturnsCreatedWithId() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.createTask(new CreateTaskRequest("Превет мир", Language.RU))).thenReturn(id);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Превет мир", "language": "RU"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/tasks/" + id)))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void createTaskRejectsUnknownLanguage() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Hello world", "language": "DE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(40001))
                .andExpect(jsonPath("$.errorMessage", containsString("Допустимые значения: [EN, RU]")))
                .andExpect(jsonPath("$.path").value("/tasks"))
                .andExpect(jsonPath("$.timestamp").exists());
        verify(taskService, never()).createTask(any());
    }

    @Test
    void createTaskRejectsMissingLanguage() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Hello world"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(40001))
                .andExpect(jsonPath("$.errorMessage", containsString("language")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "12345", "!@#$%^", "123 !!! 456", "   "})
    void createTaskRejectsInvalidText(String text) throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"" + text + "\", \"language\": \"EN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(40001))
                .andExpect(jsonPath("$.errorMessage", containsString("text")));
        verify(taskService, never()).createTask(any());
    }

    @Test
    void createTaskRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(40002));
    }

    @Test
    void getTaskReturnsCorrectedText() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.getTask(id)).thenReturn(new TaskResponse(TaskStatus.COMPLETED, "Привет мир", null));

        mockMvc.perform(get("/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.correctedText").value("Привет мир"))
                .andExpect(jsonPath("$.errorMessage").doesNotExist());
    }

    @Test
    void getTaskInProgressReturnsOnlyStatus() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.getTask(id)).thenReturn(new TaskResponse(TaskStatus.IN_PROGRESS, null, null));

        mockMvc.perform(get("/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.correctedText").doesNotExist())
                .andExpect(jsonPath("$.errorMessage").doesNotExist());
    }

    @Test
    void getTaskReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.getTask(id)).thenThrow(new TaskNotFoundException(id));

        mockMvc.perform(get("/tasks/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(40401))
                .andExpect(jsonPath("$.errorMessage").value("Задача с id: " + id + " не найдена"))
                .andExpect(jsonPath("$.path").value("/tasks/" + id));
    }

    @Test
    void getTaskRejectsInvalidId() throws Exception {
        mockMvc.perform(get("/tasks/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(40003));
    }
}
