package com.epam.training.microservicefoundation.resourceservice.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

@Table("RESOURCES")
@Value
@Builder(toBuilder = true)
public class Resource implements Serializable {
  public static final long serialVersionUID = 2022_10_06_06_57L;
  @Id
  long id;
  String key;
  String name;
  ResourceStatus status;
  long storageId;
  @CreatedDate
  LocalDateTime createdDate;
  @LastModifiedDate
  LocalDateTime lastModifiedDate;
}
