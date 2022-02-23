package org.shrtr.core.domain.repositories;

import org.shrtr.core.domain.entities.Link;
import org.shrtr.core.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinksRepository extends JpaRepository<Link, UUID> {
  Optional<Link> findAllByOriginal(String originalUrl);
  Optional<Link> findByShortened(String shortened);
  Optional<Link> findByOwnerAndId(User user, UUID id);
  List<Link> findByOwner(User user);
}
