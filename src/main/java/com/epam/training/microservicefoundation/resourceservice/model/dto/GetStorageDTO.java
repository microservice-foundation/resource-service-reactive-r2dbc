package com.epam.training.microservicefoundation.resourceservice.model.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetStorageDTO implements Serializable {
  private static final long serialVersionUID = 2023_07_15_16_56L;
  private long id;
  private String bucket;
  private String path;
  private StorageType type;
}
