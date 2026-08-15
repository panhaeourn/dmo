package com.example.demo.dto;

public record VideoViewResponse(
        long views,
        Long uniqueViewers,
        long totalWatchSeconds,
        double progressSeconds,
        boolean completed,
        boolean viewCounted
) {
}
