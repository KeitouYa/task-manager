package com.eulerity.taskmanager.service;

import com.eulerity.taskmanager.dto.TaskResponse;
import com.eulerity.taskmanager.enums.Priority;
import com.eulerity.taskmanager.enums.Status;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class OpenAIClientTest {

    private static final String URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";
    private static final String API_KEY = "sk-test-key";
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-19T12:00:00Z"), ZoneOffset.UTC);

    private MockRestServiceServer mockServer;
    private OpenAIClient openAIClient;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        RestClient restClient = RestClient.create(restTemplate);
        mapper = new ObjectMapper();
        openAIClient = new OpenAIClient(restClient, mapper, FIXED_CLOCK, API_KEY, MODEL, URL);
    }

    @Test
    void suggestTask_buildsRequestWithExpectedShape() throws Exception {
        java.util.Map<String, Object> contentMap = new java.util.HashMap<>();
        contentMap.put("title", "x");
        contentMap.put("description", null);
        contentMap.put("dueDate", null);
        contentMap.put("priority", "MEDIUM");
        contentMap.put("status", "TODO");
        String cannedContent = mapper.writeValueAsString(contentMap);
        String responseBody = mapper.writeValueAsString(java.util.Map.of(
                "choices", java.util.List.of(java.util.Map.of(
                        "message", java.util.Map.of("content", cannedContent)
                ))
        ));

        mockServer.expect(requestTo(URL))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andExpect(req -> {
                    MockClientHttpRequest mock = (MockClientHttpRequest) req;
                    JsonNode body = mapper.readTree(mock.getBodyAsBytes());
                    assertThat(body.path("model").asText()).isEqualTo(MODEL);

                    JsonNode messages = body.path("messages");
                    assertThat(messages.isArray()).isTrue();
                    assertThat(messages).hasSize(2);
                    assertThat(messages.get(0).path("role").asText()).isEqualTo("system");
                    assertThat(messages.get(0).path("content").asText())
                            .contains("Today's date is 2026-05-19");
                    assertThat(messages.get(1).path("role").asText()).isEqualTo("user");
                    assertThat(messages.get(1).path("content").asText())
                            .isEqualTo("submit the quarterly report before Friday");

                    JsonNode rf = body.path("response_format");
                    assertThat(rf.path("type").asText()).isEqualTo("json_schema");
                    JsonNode js = rf.path("json_schema");
                    assertThat(js.path("name").asText()).isEqualTo("Task");
                    assertThat(js.path("strict").asBoolean()).isTrue();

                    JsonNode schema = js.path("schema");
                    assertThat(schema.path("additionalProperties").asBoolean()).isFalse();

                    JsonNode required = schema.path("required");
                    assertThat(required).extracting(JsonNode::asText)
                            .containsExactly("title", "description", "dueDate", "priority", "status");

                    JsonNode priorityEnum = schema.path("properties").path("priority").path("enum");
                    assertThat(priorityEnum).extracting(JsonNode::asText)
                            .containsExactly("LOW", "MEDIUM", "HIGH");

                    JsonNode statusEnum = schema.path("properties").path("status").path("enum");
                    assertThat(statusEnum).extracting(JsonNode::asText)
                            .containsExactly("TODO", "IN_PROGRESS", "DONE");
                })
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        openAIClient.suggestTask("submit the quarterly report before Friday");

        mockServer.verify();
    }

    @Test
    void suggestTask_parsesResponseIntoTaskResponse() throws Exception {
        String cannedContent = mapper.writeValueAsString(java.util.Map.of(
                "title", "Submit quarterly report",
                "description", "Send Q1 summary to finance",
                "dueDate", "2026-05-22",
                "priority", "HIGH",
                "status", "TODO"
        ));
        String responseBody = mapper.writeValueAsString(java.util.Map.of(
                "choices", java.util.List.of(java.util.Map.of(
                        "message", java.util.Map.of("content", cannedContent)
                ))
        ));

        mockServer.expect(requestTo(URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        TaskResponse out = openAIClient.suggestTask("submit the quarterly report before Friday");

        assertThat(out.getId()).isNull();
        assertThat(out.getTitle()).isEqualTo("Submit quarterly report");
        assertThat(out.getDescription()).isEqualTo("Send Q1 summary to finance");
        assertThat(out.getDueDate()).isEqualTo(LocalDate.of(2026, 5, 22));
        assertThat(out.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(out.getStatus()).isEqualTo(Status.TODO);
    }
}
