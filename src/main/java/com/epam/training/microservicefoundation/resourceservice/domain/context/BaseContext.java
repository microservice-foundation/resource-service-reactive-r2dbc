package com.epam.training.microservicefoundation.resourceservice.domain.context;

import com.epam.training.microservicefoundation.resourceservice.domain.dto.GetStorageDTO;
import com.epam.training.microservicefoundation.resourceservice.domain.entity.Resource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.With;

@With
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class BaseContext {
  private Resource resource;
  private GetStorageDTO storage;

  public BaseContext(@NonNull final Resource resource) {
    this.resource = resource;
  }
}
