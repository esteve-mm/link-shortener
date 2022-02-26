package org.shrtr.core.events;

import org.shrtr.core.domain.entities.User;

public interface EventService {
    void userCreated(User user);
    void entityCreated(Object entity);
    void entityDeleted(Object entity);
    void entityUpdated(Object entity);
}
