package com.epam.training.microservicefoundation.resourceservice.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.epam.training.microservicefoundation.resourceservice.configuration.TestsMappersConfig;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.DeleteResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.domain.entity.Resource;
import com.epam.training.microservicefoundation.resourceservice.service.mapper.DeleteResourceMapper;
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
