package com.epam.training.microservicefoundation.resourceservice.model.context;

import com.epam.training.microservicefoundation.resourceservice.model.dto.GetStorageDTO;
import com.epam.training.microservicefoundation.resourceservice.model.entity.Resource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ResourcePermanentContext {
  private GetStorageDTO sourceStorage;
  private GetStorageDTO destinationStorage;
  private Resource resource;
}
