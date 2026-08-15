package com.example.demo.dto;

public record VideoListStatsResponse(
        Long videoId,
        long views,
        Long uniqueViewers
) {
}
