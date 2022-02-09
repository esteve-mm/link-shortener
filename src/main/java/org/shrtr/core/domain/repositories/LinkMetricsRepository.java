package org.shrtr.core.domain.repositories;

import org.shrtr.core.domain.entities.Link;
import org.shrtr.core.domain.entities.LinkMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkMetricsRepository extends JpaRepository<LinkMetric, UUID> {

    Optional<LinkMetric> findByLinkAndDate(Link link, LocalDate localDate);
    List<LinkMetric> findAllByDateBetweenAndLink(LocalDate from, LocalDate to, Link link);
}
