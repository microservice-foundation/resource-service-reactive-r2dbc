package com.epam.training.microservicefoundation.resourceservice.service.mapper;

import com.epam.training.microservicefoundation.resourceservice.domain.entity.Resource;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.GetResourceDTO;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapper.class)
public interface GetResourceMapper extends BaseMapper<Resource, GetResourceDTO> {
}
