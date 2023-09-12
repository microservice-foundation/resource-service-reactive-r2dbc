package com.epam.training.microservicefoundation.resourceservice.domain.context;

import com.epam.training.microservicefoundation.resourceservice.domain.dto.GetStorageDTO;
import com.epam.training.microservicefoundation.resourceservice.domain.entity.Resource;
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
