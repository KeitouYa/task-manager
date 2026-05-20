package com.eulerity.taskmanager.service;

import com.eulerity.taskmanager.dto.TaskRequest;
import com.eulerity.taskmanager.dto.TaskResponse;
import com.eulerity.taskmanager.entity.Task;
import com.eulerity.taskmanager.enums.Priority;
import com.eulerity.taskmanager.enums.Status;
import com.eulerity.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void create_persistsAndReturnsResponse() {
        TaskRequest req = new TaskRequest();
        req.setTitle("Write tests");
        req.setDescription("for the service layer");
        req.setDueDate(LocalDate.of(2026, 5, 22));
        req.setPriority(Priority.HIGH);
        req.setStatus(Status.TODO);

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        TaskResponse out = taskService.create(req);

        assertThat(out.getId()).isEqualTo(1L);
        assertThat(out.getTitle()).isEqualTo("Write tests");
        assertThat(out.getDescription()).isEqualTo("for the service layer");
        assertThat(out.getDueDate()).isEqualTo(LocalDate.of(2026, 5, 22));
        assertThat(out.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(out.getStatus()).isEqualTo(Status.TODO);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void findAll_returnsAllTasks() {
        Task t1 = task(1L, "A");
        Task t2 = task(2L, "B");
        when(taskRepository.findAll()).thenReturn(List.of(t1, t2));

        List<TaskResponse> out = taskService.findAll();

        assertThat(out).hasSize(2);
        assertThat(out).extracting(TaskResponse::getId).containsExactly(1L, 2L);
        assertThat(out).extracting(TaskResponse::getTitle).containsExactly("A", "B");
    }

    @Test
    void findById_returnsTask() {
        Task t = task(7L, "Find me");
        when(taskRepository.findById(7L)).thenReturn(Optional.of(t));

        TaskResponse out = taskService.findById(7L);

        assertThat(out.getId()).isEqualTo(7L);
        assertThat(out.getTitle()).isEqualTo("Find me");
    }

    @Test
    void update_replacesFieldsAndReturnsResponse() {
        Task existing = task(5L, "old title");
        when(taskRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskRequest req = new TaskRequest();
        req.setTitle("new title");
        req.setDescription("new desc");
        req.setDueDate(LocalDate.of(2026, 6, 1));
        req.setPriority(Priority.LOW);
        req.setStatus(Status.DONE);

        TaskResponse out = taskService.update(5L, req);

        assertThat(out.getId()).isEqualTo(5L);
        assertThat(out.getTitle()).isEqualTo("new title");
        assertThat(out.getDescription()).isEqualTo("new desc");
        assertThat(out.getDueDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(out.getPriority()).isEqualTo(Priority.LOW);
        assertThat(out.getStatus()).isEqualTo(Status.DONE);
    }

    @Test
    void delete_removesTaskWhenExists() {
        when(taskRepository.existsById(9L)).thenReturn(true);

        taskService.delete(9L);

        verify(taskRepository).deleteById(9L);
        verify(taskRepository, never()).findById(any());
    }

    private Task task(Long id, String title) {
        Task t = new Task();
        t.setId(id);
        t.setTitle(title);
        t.setPriority(Priority.MEDIUM);
        t.setStatus(Status.TODO);
        return t;
    }
}
