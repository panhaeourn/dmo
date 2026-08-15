package com.example.demo.dto;

public record VideoViewResponse(
        long views,
        long uniqueViewers,
        long totalWatchSeconds,
        double progressSeconds,
        boolean completed,
        boolean viewCounted
) {
}
