package com.epam.training.microservicefoundation.resourceservice.model;

import com.epam.training.microservicefoundation.resourceservice.model.dto.GetStorageDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.With;
import org.springframework.http.codec.multipart.FilePart;

@With
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ResourceFile {
  @NonNull
  private FilePart filePart;
  @NonNull
  private GetStorageDTO storage;
  public ResourceFile(@NonNull FilePart filePart, @NonNull GetStorageDTO storage) {
    this.filePart = filePart;
    this.storage = storage;
  }
  private String key;
  private String filename;
}
