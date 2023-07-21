package com.epam.training.microservicefoundation.resourceservice.mapper;

import com.epam.training.microservicefoundation.resourceservice.model.entity.Resource;
import com.epam.training.microservicefoundation.resourceservice.model.dto.GetResourceDTO;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapper.class)
public interface GetResourceMapper extends BaseMapper<Resource, GetResourceDTO> {
}
