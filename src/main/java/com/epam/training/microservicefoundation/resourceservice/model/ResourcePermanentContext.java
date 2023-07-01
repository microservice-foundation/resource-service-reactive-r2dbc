package com.epam.training.microservicefoundation.resourceservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ResourcePermanentContext {
  private StorageDTO sourceStorage;
  private StorageDTO destinationStorage;
  private Resource resource;
}
