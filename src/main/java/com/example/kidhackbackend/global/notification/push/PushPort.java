package com.example.kidhackbackend.global.notification.push;

public interface PushPort {
    void sendPush(String deviceToken, String title, String body);
}