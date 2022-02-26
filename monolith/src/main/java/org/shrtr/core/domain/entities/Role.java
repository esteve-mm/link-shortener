package org.shrtr.core.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@JsonIgnoreProperties("hibernateLazyInitializer")
public class Role extends BaseEntity{

  private String name;
  @ManyToMany(mappedBy = "roles")
  private Set<User> users;

}
