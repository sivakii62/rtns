package com.demo.rtns.controller;

import com.demo.rtns.entity.Notification;
import com.demo.rtns.repository.NotificationRepository;
import com.demo.rtns.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Receives Pub/Sub push messages from GCP.
 *
 * When multiple Cloud Run instances are running, task creation may land on Instance A
 * while User 2's SSE connection is on Instance B. Pub/Sub delivers the push to all
 * instances, so each instance can check its local registry and forward if the user
 * is connected there.
 *
 * GCP Pub/Sub push subscription should point to:
 *   https://<your-cloud-run-url>/internal/pubsub/notify
 *
 * TODO: Secure this endpoint (e.g. verify the Authorization header sent by Pub/Sub).
 */
@RestController
@RequestMapping("/internal/pubsub")
public class PubSubPushController {

    private static final Logger log = LoggerFactory.getLogger(PubSubPushController.class);

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public PubSubPushController(NotificationRepository notificationRepository,
                                NotificationService notificationService) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    /**
     * Pub/Sub push payload structure:
     * {
     *   "message": {
     *     "attributes": { "notificationId": "123" },
     *     "data": "<base64>",
     *     "messageId": "..."
     *   },
     *   "subscription": "projects/.../subscriptions/..."
     * }
     *
     * We store the notificationId in the Pub/Sub message attributes
     * so we can look it up and attempt SSE delivery on this instance.
     */
    @PostMapping("/notify")
    public ResponseEntity<Void> handlePush(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) body.get("message");

            @SuppressWarnings("unchecked")
            Map<String, String> attributes = (Map<String, String>) message.get("attributes");

            String notificationIdStr = attributes.get("notificationId");
            Long notificationId = Long.parseLong(notificationIdStr);

            notificationRepository.findById(notificationId).ifPresent(notification -> {
                if (notification.getStatus() == Notification.Status.PENDING) {
                    notificationService.pushIfConnected(notification.getRecipientUserId(), notification);
                }
            });

            // Always return 200 to acknowledge the Pub/Sub message
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to process Pub/Sub push: {}", e.getMessage());
            // Return 200 anyway to avoid infinite Pub/Sub retries for bad payloads
            return ResponseEntity.ok().build();
        }
    }
}
