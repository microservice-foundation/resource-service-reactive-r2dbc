package com.epam.training.microservicefoundation.resourceservice.service.mapper;

import com.epam.training.microservicefoundation.resourceservice.domain.entity.Resource;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.DeleteResourceDTO;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapper.class)
public interface DeleteResourceMapper extends BaseMapper<Resource, DeleteResourceDTO>{
}
