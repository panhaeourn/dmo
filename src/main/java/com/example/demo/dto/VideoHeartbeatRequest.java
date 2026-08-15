package com.example.demo.dto;

public record VideoHeartbeatRequest(
        String sessionId,
        double positionSeconds,
        double durationSeconds,
        boolean playing
) {
}
