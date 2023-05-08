package com.epam.training.microservicefoundation.resourceservice.model;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.s3.model.CompletedPart;

public class UploadState {
  public final String bucket;
  public final String filekey;

  public String uploadId;
  public int partCounter;
  public Map<Integer, CompletedPart> completedParts = new HashMap<>();
  public int buffered = 0;

  public UploadState(String bucket, String filekey) {
    this.bucket = bucket;
    this.filekey = filekey;
  }
}
