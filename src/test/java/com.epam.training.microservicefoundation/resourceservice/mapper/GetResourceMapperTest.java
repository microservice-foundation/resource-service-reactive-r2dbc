package com.epam.training.microservicefoundation.resourceservice.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.epam.training.microservicefoundation.resourceservice.config.TestsMappersConfig;
import com.epam.training.microservicefoundation.resourceservice.model.dto.GetResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.model.entity.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestsMappersConfig.class)
class GetResourceMapperTest {
  @Autowired
  private GetResourceMapper getResourceMapper;

  private final Resource resource = Resource.builder().id(1L).name("Cup").key("test-123").storageId(99L).build();
  private final GetResourceDTO getResourceDTO = new GetResourceDTO(1L);

  @Test
  void toDtoMapping() {
    GetResourceDTO dto = getResourceMapper.toDto(resource);
    assertEquals(getResourceDTO, dto);
  }

  @Test
  void toEntityMapping() {
    Resource entity = getResourceMapper.toEntity(getResourceDTO);
    assertEquals(resource.getId(), entity.getId());
  }
}
