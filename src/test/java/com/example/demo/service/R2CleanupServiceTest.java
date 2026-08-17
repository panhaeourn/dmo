package com.example.demo.service;

import com.example.demo.entity.R2CleanupJob;
import com.example.demo.repository.R2CleanupJobRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class R2CleanupServiceTest {

    @Test
    void deletesObjectAndRemovesCompletedJob() throws Exception {
        R2CleanupJobRepository repository = mock(R2CleanupJobRepository.class);
        FileService fileService = mock(FileService.class);
        R2CleanupService service = new R2CleanupService(repository, fileService);
        R2CleanupJob job = new R2CleanupJob();
        job.setObjectKey("lesson.mp4");
        job.setReason("course-deleted:7");
        when(repository.findTop20ByNextAttemptAtLessThanEqualOrderByIdAsc(any(Instant.class)))
                .thenReturn(List.of(job));

        service.processDueJobs();

        verify(fileService).delete("lesson.mp4");
        verify(repository).delete(job);
    }
}
