package com.eulerity.taskmanager.service;

import com.eulerity.taskmanager.dto.TaskResponse;
import com.eulerity.taskmanager.enums.Priority;
import com.eulerity.taskmanager.enums.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuggestServiceTest {

    @Mock
    private OpenAIClient openAIClient;

    @InjectMocks
    private SuggestService suggestService;

    @Test
    void suggest_delegatesToOpenAIClient() {
        TaskResponse stub = new TaskResponse();
        stub.setTitle("Submit quarterly report");
        stub.setPriority(Priority.MEDIUM);
        stub.setStatus(Status.TODO);
        when(openAIClient.suggestTask("remind me to submit the quarterly report")).thenReturn(stub);

        TaskResponse out = suggestService.suggest("remind me to submit the quarterly report");

        assertThat(out).isSameAs(stub);
        verify(openAIClient).suggestTask("remind me to submit the quarterly report");
    }
}
