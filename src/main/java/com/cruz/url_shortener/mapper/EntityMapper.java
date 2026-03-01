package com.cruz.url_shortener.mapper;

public interface EntityMapper <EntityType,RequestDtoType,ResponseDtoType>{
    EntityType toEntity(RequestDtoType requestDto);
    ResponseDtoType toResponseDto(EntityType entity);
}
