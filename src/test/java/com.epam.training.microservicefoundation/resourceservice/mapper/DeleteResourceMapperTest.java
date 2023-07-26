package com.epam.training.microservicefoundation.resourceservice.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.epam.training.microservicefoundation.resourceservice.config.TestsMappersConfig;
import com.epam.training.microservicefoundation.resourceservice.model.dto.DeleteResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.model.entity.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestsMappersConfig.class)
class DeleteResourceMapperTest {
  @Autowired
  private DeleteResourceMapper deleteResourceMapper;
  private final Resource resource = Resource.builder().id(1L).name("Cup").key("test-123").storageId(99L).build();
  private final DeleteResourceDTO deleteResourceDTO = new DeleteResourceDTO(1L);

  @Test
  void toDtoMapping() {
    DeleteResourceDTO dto = deleteResourceMapper.toDto(resource);
    assertEquals(deleteResourceDTO, dto);
  }

  @Test
  void toEntityMapping() {
    Resource entity = deleteResourceMapper.toEntity(deleteResourceDTO);
    assertEquals(resource.getId(), entity.getId());
  }
}
