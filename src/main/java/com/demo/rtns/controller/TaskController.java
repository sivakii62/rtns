package com.demo.rtns.controller;

import com.demo.rtns.dto.TaskRequest;
import com.demo.rtns.entity.Task;
import com.demo.rtns.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * User 1 creates a task for User 2.
     * Saves to DB and triggers notification to the assigned user.
     *
     * POST /tasks
     * {
     *   "title": "Fix bug #42",
     *   "description": "...",
     *   "assignedToUserId": 2,
     *   "createdByUserId": 1
     * }
     */
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody TaskRequest request) {
        Task task = taskService.createTask(request);
        return ResponseEntity.ok(task);
    }
}
