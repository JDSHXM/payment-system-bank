package com.paynetSystem.paynetSystemBank.notification.repo;

import com.paynetSystem.paynetSystemBank.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepo extends JpaRepository<Notification, Long> {

}
