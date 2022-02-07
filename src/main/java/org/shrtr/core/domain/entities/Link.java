package org.shrtr.core.domain.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "links")
@Setter
@Getter
public class Link extends BaseEntity {
  private String original;
  @Column(unique = true)
  private String shortened;
  private int counter;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private User owner;

}
