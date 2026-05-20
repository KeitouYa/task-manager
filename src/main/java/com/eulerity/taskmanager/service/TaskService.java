package com.eulerity.taskmanager.service;

import com.eulerity.taskmanager.dto.TaskRequest;
import com.eulerity.taskmanager.dto.TaskResponse;
import com.eulerity.taskmanager.entity.Task;
import com.eulerity.taskmanager.exception.TaskNotFoundException;
import com.eulerity.taskmanager.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskResponse create(TaskRequest req) {
        Task task = new Task();
        apply(req, task);
        return toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findAll() {
        return taskRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return toResponse(loadOrThrow(id));
    }

    public TaskResponse update(Long id, TaskRequest req) {
        Task task = loadOrThrow(id);
        apply(req, task);
        return toResponse(taskRepository.save(task));
    }

    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
    }

    private Task loadOrThrow(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private void apply(TaskRequest req, Task task) {
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        task.setDueDate(req.getDueDate());
        task.setPriority(req.getPriority());
        task.setStatus(req.getStatus());
    }

    private TaskResponse toResponse(Task task) {
        TaskResponse r = new TaskResponse();
        r.setId(task.getId());
        r.setTitle(task.getTitle());
        r.setDescription(task.getDescription());
        r.setDueDate(task.getDueDate());
        r.setPriority(task.getPriority());
        r.setStatus(task.getStatus());
        return r;
    }
}
