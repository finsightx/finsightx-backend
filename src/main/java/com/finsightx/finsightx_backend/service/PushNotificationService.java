package com.finsightx.finsightx_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {
    // TODO

    public void sendPushNotification(Long userId, String title, String body) {
        log.info("Push Notification Send Request: User ID = {}, Title = '{}', Body = '{}'", userId, title, body);

        // TODO

        log.info("Notification sent to user {}. Title: '{}', Body: '{}'", userId, title, body);
    }

}
