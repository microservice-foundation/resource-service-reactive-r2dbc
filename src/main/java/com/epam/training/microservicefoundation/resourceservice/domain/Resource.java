package com.epam.training.microservicefoundation.resourceservice.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"path", "name"}))
public class Resource implements Serializable {
    public static final long serialVersionUID = 2022_10_06_06_57L;
    @Id
    @GeneratedValue(generator = "resource_sequence", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "resource_sequence", sequenceName = "resource_sequence", allocationSize = 5)
    private long id;
    @Column(length = 200, nullable = false)
    private String path;
    @Column(length = 100, nullable = false)
    private String name;
    @Column(nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    protected Resource() {}
    private Resource(Builder builder) {
        this.id = builder.id;
        this.path = builder.path;
        this.name = builder.name;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", this.id);
        parameters.put("path", this.path);
        parameters.put("name", this.name);

        return parameters;
    }

    public static class Builder {
        private final String path;
        private final String name;
        private long id;

        public Builder(String path, String name) {
            this.path = path;
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
