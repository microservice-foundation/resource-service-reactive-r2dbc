package com.epam.training.microservicefoundation.resourceservice.mapper;

import com.epam.training.microservicefoundation.resourceservice.model.entity.Resource;
import com.epam.training.microservicefoundation.resourceservice.model.dto.DeleteResourceDTO;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapper.class)
public interface DeleteResourceMapper extends BaseMapper<Resource, DeleteResourceDTO>{
}
