package org.shrtr.core.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DbInterceptorConfiguration {

    private final EntityManagerFactory entityManagerFactory;
    private final EventService eventService;

    @PostConstruct
    public void configure() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);

        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(
                EventListenerRegistry.class);
        registry.appendListeners(EventType.POST_INSERT, new PostInsertEventListener() {
            @Override
            public void onPostInsert(PostInsertEvent event) {
                eventService.entityCreated(event.getEntity());
            }

            @Override
            public boolean requiresPostCommitHanding(EntityPersister persister) {
                return false;
            }
        });
        registry.appendListeners(EventType.POST_DELETE, new PostDeleteEventListener() {
            @Override
            public void onPostDelete(PostDeleteEvent event) {
                eventService.entityDeleted(event.getEntity());

            }

            @Override
            public boolean requiresPostCommitHanding(EntityPersister persister) {
                return false;
            }
        });
        registry.appendListeners(EventType.POST_UPDATE, new PostUpdateEventListener() {
            @Override
            public void onPostUpdate(PostUpdateEvent event) {
                eventService.entityUpdated(event.getEntity());
            }

            @Override
            public boolean requiresPostCommitHanding(EntityPersister persister) {
                return false;
            }
        });
    }

}