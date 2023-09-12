package com.epam.training.microservicefoundation.resourceservice.domain.event;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceProcessedEvent implements Serializable {
  private static final long serialVersionUID = 21_07_2023_22_18L;
  private long id;
}
