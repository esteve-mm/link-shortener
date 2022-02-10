package org.shrtr.core.services;

import org.shrtr.core.domain.entities.*;

public interface RateLimiting {
    boolean isEnabled(User user);
    boolean limitExceeded(Link link, User user);

}
