package com.epam.training.microservicefoundation.resourceservice.model.context;

import com.epam.training.microservicefoundation.resourceservice.model.dto.GetStorageDTO;
import com.epam.training.microservicefoundation.resourceservice.model.entity.Resource;
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
