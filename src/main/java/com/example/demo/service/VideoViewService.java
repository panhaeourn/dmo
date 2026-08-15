package com.example.demo.service;

import com.example.demo.dto.VideoHeartbeatRequest;
import com.example.demo.dto.VideoViewResponse;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.CourseVideo;
import com.example.demo.entity.VideoView;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.CourseVideoRepository;
import com.example.demo.repository.VideoViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoViewService {

    static final Duration VIEW_COOLDOWN = Duration.ofHours(6);
    static final Duration MAX_HEARTBEAT_GAP = Duration.ofSeconds(30);

    private final CourseVideoRepository courseVideoRepository;
    private final VideoViewRepository videoViewRepository;
    private final AppUserRepository appUserRepository;
    private final CourseAccessService courseAccessService;

    @Transactional
    public VideoViewResponse heartbeat(Long videoId, VideoHeartbeatRequest request, Authentication authentication) {
        validateRequest(request);

        CourseVideo video = courseVideoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        courseAccessService.requireCourseAccess(authentication, video.getCourse());
        AppUser user = appUserRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account not found"));

        VideoView view = videoViewRepository.findForUpdate(videoId, user.getId()).orElseGet(() -> newView(video, user));
        Instant now = currentTime();
        boolean sameSession = request.sessionId().equals(view.getSessionId());
        double credited = 0;

        if (!sameSession) {
            view.setSessionId(request.sessionId());
            view.setSessionWatchSeconds(0);
            view.setSessionQualified(false);
            view.setLastEventAt(now);
            view.setLastPositionSeconds(request.positionSeconds());
        } else if (view.getLastEventAt() != null) {
            double elapsed = Math.min(
                    Duration.between(view.getLastEventAt(), now).toMillis() / 1000.0,
                    MAX_HEARTBEAT_GAP.toSeconds()
            );
            double positionAdvance = request.positionSeconds() - view.getLastPositionSeconds();

            if (request.playing() && elapsed > 0 && positionAdvance > 0) {
                credited = Math.min(elapsed + 1.0, positionAdvance);
                view.setSessionWatchSeconds(view.getSessionWatchSeconds() + credited);
                view.setTotalWatchSeconds(view.getTotalWatchSeconds() + Math.round(credited));
            }

            view.setLastEventAt(now);
            view.setLastPositionSeconds(request.positionSeconds());
        }

        view.setDurationSeconds(Math.max(view.getDurationSeconds(), request.durationSeconds()));
        view.setProgressSeconds(Math.min(request.durationSeconds(), view.getProgressSeconds() + credited));
        if (request.durationSeconds() > 0 && view.getProgressSeconds() / request.durationSeconds() >= 0.90) {
            view.setCompleted(true);
        }

        boolean viewCounted = false;
        double qualificationSeconds = Math.min(30.0, request.durationSeconds() * 0.25);
        boolean cooldownPassed = view.getLastQualifiedAt() == null
                || Duration.between(view.getLastQualifiedAt(), now).compareTo(VIEW_COOLDOWN) >= 0;
        if (!view.isSessionQualified() && view.getSessionWatchSeconds() >= qualificationSeconds && cooldownPassed) {
            view.setViewCount(view.getViewCount() + 1);
            view.setLastQualifiedAt(now);
            view.setSessionQualified(true);
            viewCounted = true;
        }

        videoViewRepository.save(view);
        return response(videoId, view, viewCounted, isAdmin(authentication));
    }

    @Transactional(readOnly = true)
    public VideoViewResponse stats(Long videoId, Authentication authentication) {
        CourseVideo video = courseVideoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        courseAccessService.requireCourseAccess(authentication, video.getCourse());
        AppUser user = appUserRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account not found"));
        VideoView view = videoViewRepository.findByVideoIdAndUserId(videoId, user.getId()).orElse(null);
        return response(videoId, view, false, isAdmin(authentication));
    }

    private VideoViewResponse response(Long videoId, VideoView view, boolean counted, boolean admin) {
        return new VideoViewResponse(
                videoViewRepository.totalViews(videoId),
                admin ? videoViewRepository.uniqueViewers(videoId) : null,
                view == null ? 0 : view.getTotalWatchSeconds(),
                view == null ? 0 : view.getProgressSeconds(),
                view != null && view.isCompleted(),
                counted
        );
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private VideoView newView(CourseVideo video, AppUser user) {
        VideoView view = new VideoView();
        view.setVideo(video);
        view.setUser(user);
        return view;
    }

    Instant currentTime() {
        return Instant.now();
    }

    private void validateRequest(VideoHeartbeatRequest request) {
        if (request == null || request.sessionId() == null || request.sessionId().length() > 36) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid viewing session");
        }
        try {
            UUID.fromString(request.sessionId());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid viewing session");
        }
        if (!Double.isFinite(request.positionSeconds()) || !Double.isFinite(request.durationSeconds())
                || request.positionSeconds() < 0 || request.durationSeconds() < 1
                || request.durationSeconds() > 86_400
                || request.positionSeconds() > request.durationSeconds() + 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid video timing");
        }
    }
}
