package com.demo.rtns.service;

import com.demo.rtns.dto.NotificationPayload;
import com.demo.rtns.entity.Notification;
import com.demo.rtns.repository.NotificationRepository;

import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final SseEmitterRegistry registry;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notificationRepository,
                               SseEmitterRegistry registry,
                               ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    /**
     * Save notification as PENDING, then attempt immediate delivery via SSE.
     * If the user is offline the notification stays PENDING until they reconnect.
     */
    public void send(Long recipientUserId, NotificationPayload payload) {
        Notification notification = new Notification();
        notification.setRecipientUserId(recipientUserId);
        notification.setPayload(toJson(payload));
        notification.setStatus(Notification.Status.PENDING);
        notificationRepository.save(notification);

        pushIfConnected(recipientUserId, notification);
    }

    /**
     * Called when a user reconnects via SSE — flushes all PENDING notifications.
     */
    public void flushPending(Long userId) {
        List<Notification> pending = notificationRepository
                .findByRecipientUserIdAndStatus(userId, Notification.Status.PENDING);

        for (Notification n : pending) {
            if (pushIfConnected(userId, n)) {
                markDelivered(n);
            }
        }
    }

    /**
     * Push a single notification to the user's SSE connection if active.
     * Returns true if pushed successfully.
     */
    public boolean pushIfConnected(Long userId, Notification notification) {
        SseEmitter emitter = registry.get(userId);
        if (emitter == null) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(notification.getPayload()));
            markDelivered(notification);
            return true;
        } catch (Exception e) {
            log.warn("Failed to push SSE to userId={}: {}", userId, e.getMessage());
            return false;
        }
    }

    private void markDelivered(Notification notification) {
        notification.setStatus(Notification.Status.DELIVERED);
        notification.setDeliveredAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }
}
