package com.paynetSystem.paynetSystemBank.notification.services;

import com.paynetSystem.paynetSystemBank.auth_users.entity.User;
import com.paynetSystem.paynetSystemBank.notification.dtos.NotificationDTO;

public interface NotificationService {
    void sendEmail(NotificationDTO notificationDTO, User user);
}
