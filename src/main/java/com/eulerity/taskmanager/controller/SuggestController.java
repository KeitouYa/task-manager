package com.eulerity.taskmanager.controller;

import com.eulerity.taskmanager.dto.SuggestRequest;
import com.eulerity.taskmanager.dto.TaskResponse;
import com.eulerity.taskmanager.service.SuggestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class SuggestController {

    private final SuggestService suggestService;

    @PostMapping("/suggest")
    public TaskResponse suggest(@Valid @RequestBody SuggestRequest req) {
        return suggestService.suggest(req.getText());
    }
}
