package com.eulerity.taskmanager.dto;

import com.eulerity.taskmanager.enums.Priority;
import com.eulerity.taskmanager.enums.Status;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskResponse {

    private Long id;

    private String title;

    private String description;

    private LocalDate dueDate;

    private Priority priority;

    private Status status;
}
