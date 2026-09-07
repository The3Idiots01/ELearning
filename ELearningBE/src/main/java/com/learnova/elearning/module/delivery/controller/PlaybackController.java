package com.learnova.elearning.module.delivery.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.delivery.config.DeliveryProperties;
import com.learnova.elearning.module.delivery.dto.PlaybackIssueResult;
import com.learnova.elearning.module.delivery.dto.PlaybackResponse;
import com.learnova.elearning.module.delivery.service.PlaybackService;
import com.learnova.elearning.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** §4.6 design_us15_us17.md — cấp PlaybackTicket cho một lesson. */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/lessons/{lessonId}/playback")
@RequiredArgsConstructor
public class PlaybackController {

    private static final String CONTENT_COOKIE_PATH = "/api/v1/content";

    private final PlaybackService playbackService;
    private final DeliveryProperties deliveryProperties;

    @GetMapping
    public ResponseEntity<ApiResponse<PlaybackResponse>> issuePlayback(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent
    ) {
        Long userId = user != null ? user.getId() : null;
        PlaybackIssueResult result = playbackService.issuePlayback(courseId, lessonId, userId, userAgent);

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if (result.playbackSessionId() != null) {
            builder.header(HttpHeaders.SET_COOKIE, buildSessionCookie(result.playbackSessionId()).toString());
        }
        return builder.body(ApiResponse.success(result.response()));
    }

    private ResponseCookie buildSessionCookie(String sessionId) {
        return ResponseCookie.from(deliveryProperties.getCookieName(), sessionId)
                .httpOnly(true)
                .secure(deliveryProperties.isCookieSecure())
                .sameSite(deliveryProperties.getCookieSameSite())
                .path(CONTENT_COOKIE_PATH)
                .maxAge(deliveryProperties.getPlaybackSessionTtl())
                .build();
    }
}
