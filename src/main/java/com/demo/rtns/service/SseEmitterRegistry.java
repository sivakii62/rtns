package com.demo.rtns.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of active SSE connections keyed by userId.
 *
 * NOTE: This works for a single Cloud Run instance.
 * For multi-instance deployments, GCP Pub/Sub push (PubSubPushController)
 * broadcasts across instances so each instance only needs to manage its own connections.
 */
@Component
public class SseEmitterRegistry {

    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void register(Long userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));
    }

    public SseEmitter get(Long userId) {
        return emitters.get(userId);
    }

    public boolean isConnected(Long userId) {
        return emitters.containsKey(userId);
    }
}
