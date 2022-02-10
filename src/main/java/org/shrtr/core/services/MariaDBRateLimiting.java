package org.shrtr.core.services;

import lombok.RequiredArgsConstructor;
import org.shrtr.core.controllers.TooManyRequestsException;
import org.shrtr.core.domain.repositories.LinksRepository;
import org.springframework.stereotype.Service;
import org.shrtr.core.domain.entities.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MariaDBRateLimiting implements RateLimiting {

    private final LinksRepository linksRepository;

    // Not quite a reliable system as we are disregarding all previous requests once
    // a time window has elapsed.

    @Override
    public boolean isEnabled(User user) {
        return user.hasRedirectRateLimit();
    }

    @Override
    public boolean limitExceeded(Link link, User user) {
        boolean exceeded = false;
        LocalDateTime now = LocalDateTime.now();
        if (link.getRateLimitWindowStart() == null){
            // first redirect ever
            link.setRateLimitWindowStart(now);
            link.setRedirectCounter(1);
        }
        else if (Duration.between(link.getRateLimitWindowStart(), now).toMillis() > user.getMaxRequestsWindowMs()) {
            // expired window
            link.setRateLimitWindowStart(now);
            link.setRedirectCounter(1);
        }
        else {
            // current window
            if (link.getRedirectCounter() >= user.getMaxRequests()){
                exceeded = true;
            }
            link.setRedirectCounter(link.getRedirectCounter() + 1);
        }

        linksRepository.save(link);
        return exceeded;
    }
}
