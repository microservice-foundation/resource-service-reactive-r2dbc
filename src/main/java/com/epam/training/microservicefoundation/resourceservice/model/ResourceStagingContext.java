package com.epam.training.microservicefoundation.resourceservice.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.http.codec.multipart.FilePart;
@Getter
@AllArgsConstructor
@NoArgsConstructor
@With
public class ResourceStagingContext {
  private FilePart filePart;
  private StorageDTO storage;
  private Resource resource;
}
