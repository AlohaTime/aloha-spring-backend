package com.aloha.time.mapper;

import org.mapstruct.IterableMapping;
import org.mapstruct.Named;

import java.util.List;

public interface GenericMapper<E, D> {
    E toEntity(D dto);

    @Named(value = "toDto")
    D toDto(E entity);

    List<E> toEntityList(List<D> dtoList);

    @IterableMapping(qualifiedByName = "toDto")
    List<D> toDtoList(List<E> entityList);
}