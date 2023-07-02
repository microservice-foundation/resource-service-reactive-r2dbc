package com.epam.training.microservicefoundation.resourceservice.common;

import com.epam.training.microservicefoundation.resourceservice.model.StorageType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class StorageTypeDeserializer extends JsonDeserializer<StorageType> {
  @Override
  public StorageType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String value = p.getValueAsString();
    return StorageType.valueOf(value.toUpperCase());
  }
}
