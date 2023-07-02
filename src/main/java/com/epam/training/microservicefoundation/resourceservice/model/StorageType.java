package com.epam.training.microservicefoundation.resourceservice.model;

import com.epam.training.microservicefoundation.resourceservice.common.StorageTypeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = StorageTypeDeserializer.class)
public enum StorageType {
  PERMANENT,
  STAGING;
}
