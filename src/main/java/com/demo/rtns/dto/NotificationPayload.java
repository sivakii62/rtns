package com.demo.rtns.dto;

public record NotificationPayload(
        Long taskId,
        String title,
        String description,
        Long assignedToUserId,
        Long createdByUserId
) {}
