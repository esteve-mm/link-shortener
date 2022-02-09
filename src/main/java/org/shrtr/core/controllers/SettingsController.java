package org.shrtr.core.controllers;

import lombok.*;
import org.shrtr.core.domain.entities.User;
import org.shrtr.core.services.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final UserService userService;

    @GetMapping("/rate-limit")
    public RateLimitSettingsDto getRateLimitSettings(@AuthenticationPrincipal User user) {
        return RateLimitSettingsDto.fromUser(user);
    }

    @PostMapping("/rate-limit")
    public RateLimitSettingsDto setRateLimitSettings(@RequestBody RateLimitSettingsDto settingsDto,
                                                        @AuthenticationPrincipal User user) {

        user = userService.updateUserRateLimitSettings(user, settingsDto);
        return RateLimitSettingsDto.fromUser(user);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimitSettingsDto {

        private long maxRequests;
        private long maxRequestsWindowMs;

        static RateLimitSettingsDto fromUser(User user) {
            return RateLimitSettingsDto.builder()
                    .maxRequests(user.getMaxRequests())
                    .maxRequestsWindowMs(user.getMaxRequestsWindowMs())
                    .build();
        }
    }

}



