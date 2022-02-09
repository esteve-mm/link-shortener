package org.shrtr.core.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shrtr.core.domain.entities.Link;
import org.shrtr.core.domain.entities.LinkMetric;
import org.shrtr.core.domain.entities.User;
import org.shrtr.core.domain.repositories.LinkMetricsRepository;
import org.shrtr.core.domain.repositories.LinksRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkService {
  private final LinksRepository linksRepository;
  private final LinkMetricsRepository linkMetricsRepository;

  @Transactional
  public Link create(String targetUrl, User user) {

    Link link = new Link();
    link.setOriginal(targetUrl);

    link.setOwner(user);
    link.setCounter(0);
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

    Optional<Link> byShortened = linksRepository.findByShortened(shortened);

    if (byShortened.isEmpty()) {
      return byShortened;
    }

    var link = byShortened.get();
    LocalDate date = LocalDate.now();
    Optional<LinkMetric> byLinkAndDate = linkMetricsRepository.findByLinkAndDate(link, date);
    LinkMetric linkMetric;
    if (byLinkAndDate.isPresent()) {
      linkMetric = byLinkAndDate.get();
      linkMetric.setCount(linkMetric.getCount() + 1);
    }
    else {
      linkMetric = new LinkMetric();
      linkMetric.setLink(link);
      linkMetric.setDate(date);
      linkMetric.setCount(1);
    }
    log.info("Count of {} is {}", link.getShortened(), linkMetric.getCount());
    linkMetricsRepository.save(linkMetric);

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

  public List<LinkMetric> findLinkMetrics(Link link, LocalDate from, LocalDate to) {
    return linkMetricsRepository.findAllByDateBetweenAndLink(from, to, link);
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
