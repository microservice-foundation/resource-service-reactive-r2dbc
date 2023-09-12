package com.epam.training.microservicefoundation.resourceservice.domain.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetResourceDTO extends AuditableDTO implements Serializable {
  private static final long serialVersionUID = 2023_09_11_11_10L;
  private long id;
}
