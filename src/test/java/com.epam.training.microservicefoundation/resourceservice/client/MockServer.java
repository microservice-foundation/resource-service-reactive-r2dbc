package com.epam.training.microservicefoundation.resourceservice.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.springframework.http.HttpStatus;

public final class MockServer {
  private final MockWebServer server;
  private final ObjectMapper mapper;

  private MockServer() throws IOException {
    this(0);
  }
  private MockServer(int port) throws IOException {
    this(InetAddress.getByName("localhost"), port);
  }

  private MockServer(InetAddress address, int port) throws IOException {
    mapper = new ObjectMapper();
    server = new MockWebServer();
    server.start(address, port);
  }

  public <T> void response(HttpStatus status, T responseBody, Map<String, String> headers) {
    MockResponse response = new MockResponse();
    response.setResponseCode(status.value());
    response.setBody(toJson(responseBody));
    headers.forEach(response::addHeader);
    server.enqueue(response);
  }

  public void response(HttpStatus status, Buffer responseBody, Map<String, String> headers) {
    MockResponse response = new MockResponse();
    response.setResponseCode(status.value());
    response.setBody(responseBody);
    headers.forEach(response::addHeader);
    server.enqueue(response);
  }

  public <T> void response(HttpStatus status, T responseBody, Map<String, String> headers, Duration duration) {
    MockResponse response = new MockResponse();
    response.setBodyDelay(duration.toMillis(), TimeUnit.MILLISECONDS);
    response.setResponseCode(status.value());
    response.setBody(toJson(responseBody));
    headers.forEach(response::addHeader);
    server.enqueue(response);
  }

  public <T> void response(HttpStatus status) {
    MockResponse response = new MockResponse();
    response.setResponseCode(status.value());
    server.enqueue(response);
  }

  private <T> String toJson(T value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public void dispose() throws IOException {
    server.close();
  }

  public static MockServer newInstance() throws IOException {
    return new MockServer();
  }

  public static MockServer newInstance(int port) throws IOException {
    return new MockServer(port);
  }

  public static MockServer newInstance(InetAddress inetAddress, int port) throws IOException {
    return new MockServer(inetAddress, port);
  }

}
