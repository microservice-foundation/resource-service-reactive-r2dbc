package com.epam.training.microservicefoundation.resourceservice.domain.context;


import com.epam.training.microservicefoundation.resourceservice.domain.dto.GetStorageDTO;
import com.epam.training.microservicefoundation.resourceservice.domain.entity.Resource;
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
  private GetStorageDTO storage;
  private Resource resource;
}
