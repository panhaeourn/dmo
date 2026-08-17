package com.example.demo.controller;

import com.example.demo.dto.QueueOrphanCleanupRequest;
import com.example.demo.dto.StorageOrphanReport;
import com.example.demo.service.StorageOrphanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/storage")
@RequiredArgsConstructor
public class AdminStorageController {
    private final StorageOrphanService storageOrphanService;

    @GetMapping("/orphans")
    public StorageOrphanReport findOrphans(
            @RequestParam(defaultValue = "24") int minimumAgeHours
    ) throws Exception {
        return storageOrphanService.report(minimumAgeHours);
    }

    @PostMapping("/orphans/queue")
    public Map<String, Integer> queueOrphans(@RequestBody QueueOrphanCleanupRequest request) throws Exception {
        int minimumAgeHours = request.minimumAgeHours() == null ? 24 : request.minimumAgeHours();
        return Map.of("queued", storageOrphanService.queue(request.objectKeys(), minimumAgeHours));
    }
}
