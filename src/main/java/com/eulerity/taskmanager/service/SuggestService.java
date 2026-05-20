package com.eulerity.taskmanager.service;

import com.eulerity.taskmanager.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SuggestService {

    private final OpenAIClient openAIClient;

    public TaskResponse suggest(String userText) {
        return openAIClient.suggestTask(userText);
    }
}
