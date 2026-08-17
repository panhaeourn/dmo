package com.example.demo.dto;

import java.util.List;

public record QueueOrphanCleanupRequest(List<String> objectKeys, Integer minimumAgeHours) {
}
