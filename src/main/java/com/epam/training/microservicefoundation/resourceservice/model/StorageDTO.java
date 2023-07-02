package com.epam.training.microservicefoundation.resourceservice.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder
@EqualsAndHashCode
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class StorageDTO implements Serializable {
  private static final long serialVersionUID = 2023_05_29_11_37L;
  private long id;
  private String bucket;
  private String path;
  private StorageType type;
}
