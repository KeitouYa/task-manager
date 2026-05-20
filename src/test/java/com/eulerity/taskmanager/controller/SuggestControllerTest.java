package com.eulerity.taskmanager.controller;

import com.eulerity.taskmanager.dto.TaskResponse;
import com.eulerity.taskmanager.enums.Priority;
import com.eulerity.taskmanager.enums.Status;
import com.eulerity.taskmanager.service.SuggestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuggestController.class)
class SuggestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SuggestService suggestService;

    @Test
    void suggest_returnsTaskResponseJson() throws Exception {
        TaskResponse stub = new TaskResponse();
        stub.setTitle("Submit quarterly report");
        stub.setDescription("Send Q1 summary to finance");
        stub.setDueDate(LocalDate.of(2026, 5, 22));
        stub.setPriority(Priority.HIGH);
        stub.setStatus(Status.TODO);

        when(suggestService.suggest(eq("remind me to submit the quarterly report before Friday")))
                .thenReturn(stub);

        String body = objectMapper.writeValueAsString(
                Map.of("text", "remind me to submit the quarterly report before Friday"));

        mockMvc.perform(post("/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.title").value("Submit quarterly report"))
                .andExpect(jsonPath("$.description").value("Send Q1 summary to finance"))
                .andExpect(jsonPath("$.dueDate").value("2026-05-22"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }
}
