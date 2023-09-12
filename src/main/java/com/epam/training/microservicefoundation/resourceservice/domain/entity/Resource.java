package com.epam.training.microservicefoundation.resourceservice.domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Table("RESOURCES")
public class Resource implements Serializable {
  public static final long serialVersionUID = 2022_10_06_06_57L;
  @Id
  private long id;
  private String key;
  private String name;
  private ResourceStatus status;
  private long storageId;
  @CreatedDate
  private LocalDateTime createdDate;
  @LastModifiedDate
  private LocalDateTime lastModifiedDate;
}
