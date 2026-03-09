package com.demo.rtns.repository;

import com.demo.rtns.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdAndStatus(Long recipientUserId, Notification.Status status);
}
