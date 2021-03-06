package org.shrtr.core.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "links")
@Setter
@Getter
@JsonIgnoreProperties("hibernateLazyInitializer")
public class Link extends BaseEntity {
  private String original;
  @Column(unique = true)
  private String shortened;
  private int counter;

  @Column(name = "redirect_counter", nullable = false)
  private int redirectCounter;

  @Column(name = "rate_limit_window_start")
  private LocalDateTime rateLimitWindowStart;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private User owner;

}
