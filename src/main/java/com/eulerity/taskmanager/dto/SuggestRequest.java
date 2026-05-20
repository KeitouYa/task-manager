package com.eulerity.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuggestRequest {

    @NotBlank
    private String text;
}
