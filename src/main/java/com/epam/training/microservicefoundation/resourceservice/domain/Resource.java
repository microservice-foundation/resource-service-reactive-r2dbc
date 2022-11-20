package com.epam.training.microservicefoundation.resourceservice.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Resource implements Serializable {
    public static final long serialVersionUID = 2022_10_06_06_57L;
    private final long id;
    private final String path;
    private final String name;
    private final LocalDateTime createdDate;
    private final LocalDateTime lastModifiedDate;

    private Resource(Builder builder) {
        this.id = builder.id;
        this.path = builder.path;
        this.name = builder.name;
        this.createdDate = builder.createdDate;
        this.lastModifiedDate = builder.lastModifiedDate;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", this.id);
        parameters.put("path", this.path);
        parameters.put("name", this.name);

        if(createdDate != null) {
            parameters.put("created_date", this.createdDate);
        }
        if(lastModifiedDate != null) {
            parameters.put("last_modified_date", this.lastModifiedDate);
        }

        return parameters;
    }

    public static class Builder {
        private final String path;
        private final String name;
        private long id;
        private LocalDateTime createdDate;
        private LocalDateTime lastModifiedDate;

        public Builder(String path, String name) {
            this.path = path;
            this.name = name;
        }

        public Builder id(long id) {
            this.id = id;

            return this;
        }

        public Builder createdDate(LocalDateTime date) {
            this.createdDate = date;
            return this;
        }

        public Builder lastModifiedDate(LocalDateTime lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
            return this;
        }

        public Resource build() {
            return new Resource(this);
        }
    }
    public long getId() {
        return id;
    }

    public String getPath() {
        return path;
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
        if(id != 0L) {
            result += 31 * Long.hashCode(id);
        }
        if(path != null) {
            result += 31 * path.hashCode();
        }
        if(name != null) {
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
        return Objects.equals(this.id, resource.id) && Objects.equals(this.path, resource.path)
                && Objects.equals(this.name, resource.name);
    }

    @Override
    public String toString() {
        return "SongResource{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", createdDate=" + createdDate +
                ", lastModifiedDate=" + lastModifiedDate +
                '}';
    }
}
