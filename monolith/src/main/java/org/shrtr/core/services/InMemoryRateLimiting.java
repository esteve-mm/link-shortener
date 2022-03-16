package org.shrtr.core.services;

import lombok.*;
import org.shrtr.core.domain.entities.Link;
import org.shrtr.core.domain.entities.User;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

@Primary
@Service
@RequiredArgsConstructor
public class InMemoryRateLimiting implements RateLimiting {

    HashMap<UUID, Pair> linksRequests;

    @PostConstruct
    private void init() {
        linksRequests = new HashMap<>();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @RequiredArgsConstructor
    private static class Pair {
        private LocalDateTime windowStart;
        private Long requests;
    }

    @Override
    public boolean isEnabled(User user) {
        return user.hasRedirectRateLimit();
    }

    @Override
    public boolean limitExceeded(Link link, User user) {
        boolean exceeded = false;

        var linkRequests = linksRequests.get(link.getId());

        if (linkRequests == null){
            // first redirect ever
            linksRequests.put(link.getId(), new Pair(LocalDateTime.now(), 1L));
        }
        else if (Duration.between(linkRequests.windowStart, LocalDateTime.now()).toMillis() > user.getMaxRequestsWindowMs()) {
            // expired window
            linkRequests.setRequests(1L);
            linkRequests.setWindowStart( LocalDateTime.now());
        }
        else {
            // current window
            if (linkRequests.getRequests() >= user.getMaxRequests()){
                exceeded = true;
            }
            linkRequests.setRequests(linkRequests.getRequests() + 1);
        }

        return exceeded;
    }
}
