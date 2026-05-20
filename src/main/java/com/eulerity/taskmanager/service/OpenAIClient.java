package com.eulerity.taskmanager.service;

import com.eulerity.taskmanager.dto.TaskResponse;
import com.eulerity.taskmanager.enums.Priority;
import com.eulerity.taskmanager.enums.Status;
import com.eulerity.taskmanager.exception.AiUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

@Service
public class OpenAIClient {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You convert a user's natural-language note into a structured task.
            Today's date is %s (ISO-8601).
            Rules:
            - title: short, action-oriented.
            - description: optional context; null if not implied.
            - dueDate: ISO-8601 (yyyy-MM-dd). Resolve relative phrases ("Friday", "next week") using today's date. null if no date is implied.
            - priority: LOW, MEDIUM, or HIGH. Default MEDIUM if not implied.
            - status: always TODO (this is a new task preview).
            Return only the structured JSON; no prose.
            """;

    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final String apiKey;
    private final String model;
    private final String url;

    public OpenAIClient(RestClient openAiRestClient,
                        ObjectMapper objectMapper,
                        Clock clock,
                        @Value("${openai.api-key:}") String apiKey,
                        @Value("${openai.model}") String model,
                        @Value("${openai.url}") String url) {
        this.restClient = openAiRestClient;
        this.mapper = objectMapper;
        this.clock = clock;
        this.apiKey = apiKey;
        this.model = model;
        this.url = url;
    }

    public TaskResponse suggestTask(String userText) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiUnavailableException("OpenAI API key not configured. Set the OPENAI_API_KEY environment variable.");
        }

        ObjectNode requestBody = buildRequestBody(userText);

        JsonNode response;
        try {
            response = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new AiUnavailableException("OpenAI request failed: " + e.getMessage(), e);
        }

        return parseResponse(response);
    }

    private ObjectNode buildRequestBody(String userText) {
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, LocalDate.now(clock));

        ArrayNode messages = mapper.createArrayNode();
        messages.add(mapper.createObjectNode().put("role", "system").put("content", systemPrompt));
        messages.add(mapper.createObjectNode().put("role", "user").put("content", userText));

        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.set("messages", messages);
        body.set("response_format", buildResponseFormat());
        return body;
    }

    private ObjectNode buildResponseFormat() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);

        ObjectNode props = mapper.createObjectNode();
        props.set("title", typeOf("string"));
        props.set("description", nullableType("string"));
        props.set("dueDate", nullableType("string"));
        props.set("priority", enumOf(Arrays.stream(Priority.values()).map(Enum::name).toArray(String[]::new)));
        props.set("status", enumOf(Arrays.stream(Status.values()).map(Enum::name).toArray(String[]::new)));
        schema.set("properties", props);

        ArrayNode required = mapper.createArrayNode();
        required.add("title").add("description").add("dueDate").add("priority").add("status");
        schema.set("required", required);

        ObjectNode jsonSchema = mapper.createObjectNode();
        jsonSchema.put("name", "Task");
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", schema);

        ObjectNode wrapper = mapper.createObjectNode();
        wrapper.put("type", "json_schema");
        wrapper.set("json_schema", jsonSchema);
        return wrapper;
    }

    private ObjectNode typeOf(String type) {
        return mapper.createObjectNode().put("type", type);
    }

    private ObjectNode nullableType(String type) {
        ArrayNode types = mapper.createArrayNode().add(type).add("null");
        ObjectNode node = mapper.createObjectNode();
        node.set("type", types);
        return node;
    }

    private ObjectNode enumOf(String[] values) {
        ObjectNode node = mapper.createObjectNode().put("type", "string");
        ArrayNode arr = mapper.createArrayNode();
        for (String v : values) arr.add(v);
        node.set("enum", arr);
        return node;
    }

    private TaskResponse parseResponse(JsonNode response) {
        try {
            String content = response
                    .path("choices").path(0)
                    .path("message").path("content")
                    .asText();
            if (content == null || content.isBlank()) {
                throw new AiUnavailableException("OpenAI returned empty content");
            }
            Map<String, Object> raw = mapper.readValue(content, Map.class);

            TaskResponse out = new TaskResponse();
            out.setTitle((String) raw.get("title"));
            out.setDescription((String) raw.get("description"));
            String due = (String) raw.get("dueDate");
            out.setDueDate(due == null ? null : LocalDate.parse(due));
            out.setPriority(Priority.valueOf((String) raw.get("priority")));
            out.setStatus(Status.valueOf((String) raw.get("status")));
            return out;
        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new AiUnavailableException("Failed to parse OpenAI response: " + e.getMessage(), e);
        }
    }
}
