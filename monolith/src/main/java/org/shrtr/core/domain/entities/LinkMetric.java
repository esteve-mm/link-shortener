package org.shrtr.core.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "link_metrics")
@Setter
@Getter
@JsonIgnoreProperties("hibernateLazyInitializer")
public class LinkMetric extends BaseEntity {

    private LocalDate date;

    private long count = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id")
    private Link link;
}
