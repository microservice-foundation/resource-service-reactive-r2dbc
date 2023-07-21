package com.epam.training.microservicefoundation.resourceservice.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceProcessedEvent {
  private long id;
}
