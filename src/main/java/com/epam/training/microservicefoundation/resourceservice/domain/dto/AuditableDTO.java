package com.epam.training.microservicefoundation.resourceservice.domain.dto;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AuditableDTO {
  private String lastModifiedBy;
  private Date lastModifiedDate;
}
