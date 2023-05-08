package com.epam.training.microservicefoundation.resourceservice.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

@Table("RESOURCES")
public class Resource implements Serializable {
  public static final long serialVersionUID = 2022_10_06_06_57L;
  @Id
  private long id;
  private String key;
  private String name;
  @CreatedDate
  private LocalDateTime createdDate;
  @LastModifiedDate
  private LocalDateTime lastModifiedDate;

  protected Resource() {
  }

  private Resource(Builder builder) {
    this.id = builder.id;
    this.key = builder.key;
    this.name = builder.name;
  }

  public Map<String, Object> toMap() {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("id", this.id);
    parameters.put("path", this.key);
    parameters.put("name", this.name);

    return parameters;
  }

  public static class Builder {
    private final String key;
    private final String name;
    private long id;

    public Builder(String key, String name) {
      this.key = key;
      this.name = name;
    }

    public Builder id(long id) {
      this.id = id;

      return this;
    }

    public Resource build() {
      return new Resource(this);
    }
  }

  public long getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public LocalDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  @Override
  public int hashCode() {
    int result = 17;
    if (id != 0L) {
      result += 31 * Long.hashCode(id);
    }
    if (key != null) {
      result += 31 * key.hashCode();
    }
    if (name != null) {
      result += 31 * name.hashCode();
    }
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Resource resource = (Resource) obj;
    return Objects.equals(this.id, resource.id) && Objects.equals(this.key, resource.key)
        && Objects.equals(this.name, resource.name);
  }

  @Override
  public String toString() {
    return "SongResource{" +
        "id=" + id +
        ", key='" + key + '\'' +
        ", name='" + name + '\'' +
        ", createdDate=" + createdDate +
        ", lastModifiedDate=" + lastModifiedDate +
        '}';
  }
}
