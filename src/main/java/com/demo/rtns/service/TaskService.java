package com.demo.rtns.service;

import com.demo.rtns.dto.NotificationPayload;
import com.demo.rtns.dto.TaskRequest;
import com.demo.rtns.entity.Task;
import com.demo.rtns.repository.TaskRepository;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    public TaskService(TaskRepository taskRepository, NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
    }

    public Task createTask(TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setAssignedToUserId(request.assignedToUserId());
        task.setCreatedByUserId(request.createdByUserId());
        Task saved = taskRepository.save(task);

        // Notify the assigned user — persisted as PENDING, pushed immediately if online
        NotificationPayload payload = new NotificationPayload(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getAssignedToUserId(),
                saved.getCreatedByUserId()
        );
        notificationService.send(saved.getAssignedToUserId(), payload);

        return saved;
    }
}
