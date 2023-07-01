package com.epam.training.microservicefoundation.resourceservice.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceStagedEvent implements Serializable {
    private static final long serialVersionUID = 10_11_2022_11_13L;
    private long id;
}
