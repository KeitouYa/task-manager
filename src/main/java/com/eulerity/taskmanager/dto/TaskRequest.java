package com.eulerity.taskmanager.dto;

import com.eulerity.taskmanager.enums.Priority;
import com.eulerity.taskmanager.enums.Status;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequest {

    @NotBlank
    private String title;

    private String description;

    private LocalDate dueDate;

    private Priority priority;

    private Status status;
}
