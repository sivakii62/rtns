package com.demo.rtns.controller;

import com.demo.rtns.service.NotificationService;
import com.demo.rtns.service.SseEmitterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final SseEmitterRegistry registry;
    private final NotificationService notificationService;

    public NotificationController(SseEmitterRegistry registry, NotificationService notificationService) {
        this.registry = registry;
        this.notificationService = notificationService;
    }

    /**
     * User 2 subscribes to their notification stream.
     * Angular opens this on login and keeps it alive.
     * On connect, all PENDING notifications are flushed immediately.
     *
     * GET /notifications/stream?userId=2
     *
     * Angular example:
     *   const es = new EventSource('/notifications/stream?userId=2');
     *   es.addEventListener('notification', e => console.log(e.data));
     */
    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam Long userId) {
        // Timeout set to 30 minutes; Angular's EventSource will auto-reconnect
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        registry.register(userId, emitter);
        log.info("SSE connection registered for userId={}", userId);

        // Flush all notifications that arrived while user was offline
        notificationService.flushPending(userId);

        return emitter;
    }
}
