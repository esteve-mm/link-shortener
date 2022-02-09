package org.shrtr.core.domain.entities;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "link_metrics")
@Setter
@Getter
public class LinkMetric extends BaseEntity {

    private LocalDate date;

    private long count = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id")
    private Link link;
}
