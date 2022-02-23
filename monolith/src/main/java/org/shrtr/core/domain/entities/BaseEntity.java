package org.shrtr.core.domain.entities;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public class BaseEntity {
  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Column(updatable = false, nullable = false ,columnDefinition = "binary(16)")
  private UUID id;

  @Column(name = "created_on")
  @CreationTimestamp
  private LocalDateTime createdOn;

  @Column(name = "updated_on")
  @UpdateTimestamp
  private LocalDateTime updatedOn;

}