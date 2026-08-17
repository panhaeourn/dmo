package com.example.demo.service;

import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.CourseVideoRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StorageOrphanServiceTest {

    @Test
    void reportsOnlyOldUnreferencedObjects() throws Exception {
        FileService fileService = mock(FileService.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseVideoRepository videoRepository = mock(CourseVideoRepository.class);
        when(videoRepository.findAllStoredObjectKeys()).thenReturn(List.of("active.mp4"));
        when(courseRepository.findAllLegacyVideoObjectKeys()).thenReturn(List.of());
        when(courseRepository.findAllTeacherPhotoObjectKeys()).thenReturn(List.of());
        when(fileService.listObjects()).thenReturn(List.of(
                new FileService.StoredObject("active.mp4", 100, Instant.now().minus(3, ChronoUnit.DAYS)),
                new FileService.StoredObject("recent.mp4", 200, Instant.now().minus(2, ChronoUnit.HOURS)),
                new FileService.StoredObject("orphan.mp4", 300, Instant.now().minus(3, ChronoUnit.DAYS))
        ));
        StorageOrphanService service = new StorageOrphanService(
                fileService, courseRepository, videoRepository, mock(R2CleanupService.class)
        );

        var report = service.report(24);

        assertThat(report.orphans()).extracting("objectKey").containsExactly("orphan.mp4");
        assertThat(report.orphanBytes()).isEqualTo(300);
    }
}
