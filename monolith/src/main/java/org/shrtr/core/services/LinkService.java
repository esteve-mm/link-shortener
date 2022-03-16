package org.shrtr.core.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shrtr.core.controllers.TooManyRequestsException;
import org.shrtr.core.domain.entities.Link;
import org.shrtr.core.domain.entities.User;
import org.shrtr.core.domain.repositories.LinksRepository;
import org.shrtr.core.events.EventService;
import org.shrtr.core.events.LinkRedirectedEvent;
import org.springframework.data.jpa.repository.query.Jpa21Utils;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalField;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkService {
  private final LinksRepository linksRepository;
  private final EventService eventService;
  private final RateLimiting rateLimiting;

  @Transactional
  public Link create(String targetUrl, User user) {

    Link link = new Link();
    link.setOriginal(targetUrl);

    link.setOwner(user);
    link.setCounter(0);
    link.setRedirectCounter(0);
    link.setShortened(randomStringAlphaNumeric(8));
    linksRepository.save(link);
    return link;
  }

  @Transactional
  public Optional<Link> findLinkByShortened(String shortened) {
    return linksRepository.findByShortened(shortened);
  }

  @Transactional
  public Optional<Link> findForRedirect(String shortened) {
    var start = System.currentTimeMillis();

    Optional<Link> byShortened = linksRepository.findByShortened(shortened);

    if (byShortened.isEmpty()) {
      return byShortened;
    }

    Link link = byShortened.get();
    User owner = link.getOwner();

    assertRateLimitIsNotExceeded(link, owner);
    var latency = System.currentTimeMillis() - start;
    emitRedirectEvent(link, latency);
    return byShortened;
  }

  @Transactional
  public List<Link> getAllLinks(User user) {
    return linksRepository.findByOwner(user);
  }

  @Transactional
  public Optional<Link> getLink(User user, UUID id) {
    return linksRepository.findByOwnerAndId(user, id);
  }

  @Transactional
  public Optional<Link> deleteLink(User user, UUID id) {
    return linksRepository.findByOwnerAndId(user, id)
            .stream()
            .peek(linksRepository::delete)
            .findAny();
  }

  private void assertRateLimitIsNotExceeded(Link link, User user) {
    if (!rateLimiting.isEnabled(user))
      return;

    if (rateLimiting.limitExceeded(link, user))
      throw new TooManyRequestsException();
  }

  private void emitRedirectEvent(Link link, Long latency) {
    LocalDateTime timestamp = LocalDateTime.now();
    var event = new LinkRedirectedEvent(timestamp, link.getId(), link.getOriginal(), link.getShortened(), link.getOwner().getUsername(), latency);
    eventService.linkRedirected(event);
  }

  private static String randomStringAlphaNumeric(int size) {
    return randomString(AB, size);
  }
  private static String randomString(String candidates, int size) {
    SecureRandom secureRandom = new SecureRandom();
    StringBuilder sb = new StringBuilder(size);
    for( int i = 0; i < size; i++ )
      sb.append( candidates.charAt( secureRandom.nextInt(candidates.length()) ) );
    return sb.toString();
  }

  private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";


}
