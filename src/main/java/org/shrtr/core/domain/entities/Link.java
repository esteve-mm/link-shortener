package org.shrtr.core.domain.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

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

  @OneToMany(mappedBy = "link", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private List<LinkMetric> metrics;

}
