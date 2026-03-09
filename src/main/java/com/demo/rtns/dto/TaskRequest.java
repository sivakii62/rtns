package com.demo.rtns.dto;

public record TaskRequest(
        String title,
        String description,
        Long assignedToUserId,
        Long createdByUserId
) {}
