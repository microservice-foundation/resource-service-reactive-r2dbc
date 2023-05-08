package com.epam.training.microservicefoundation.resourceservice.repository.s3storage;

import java.io.File;
import java.nio.file.Path;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import reactor.core.publisher.Mono;

public class MockFilePart implements FilePart {

  private final String filename;
  private final byte[] content;
  private final HttpHeaders headers;

  public MockFilePart(String filename, byte[] content, HttpHeaders headers) {
    this.filename = filename;
    this.content = content;
    this.headers = headers;
  }

  @Override
  public String filename() {
    return filename;
  }

  @Override
  public String name() {
    return null;
  }

  @Override
  public HttpHeaders headers() {
    return headers;
  }

  public long size() {
    return content.length;
  }

  @Override
  public Flux<DataBuffer> content() {
    return Flux.just(new DefaultDataBufferFactory().wrap(ByteBuffer.wrap(content)));
  }

  public InputStream contentStream() throws IOException {
    return new ByteArrayInputStream(content);
  }

  public String contentType() {
    return headers.getContentType().toString();
  }

  public boolean isCached() {
    return false;
  }

  @Override
  public Mono<Void> transferTo(File dest) {
    return null;
  }

  @Override
  public Mono<Void> transferTo(Path dest) {
    return null;
  }

  public void transferTo(MultipartFile file) throws IOException, IllegalStateException {
    throw new UnsupportedOperationException();
  }
}
