package com.cruz.url_shortener.mapper;

import com.cruz.url_shortener.config.AppProperties;
import com.cruz.url_shortener.dto.UrlRequestDto;
import com.cruz.url_shortener.dto.UrlResponseDto;
import com.cruz.url_shortener.entity.Url;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;



@Component
@RequiredArgsConstructor
public class UrlMapper implements EntityMapper<Url, UrlRequestDto, UrlResponseDto> {
    @Override
    public Url toEntity(UrlRequestDto urlRequestDto){
        if(urlRequestDto == null){
            return null;
        }
        var url = new Url();
        url.setLongUrl(urlRequestDto.getLongUrl());
        return url;
    };
@Override
 public UrlResponseDto toResponseDto(Url url) {
     if(url == null){
         return null;
     }
        var responseDto = new UrlResponseDto();
        responseDto.setShortUrl(url.getShortCode());
        responseDto.setLongUrl(url.getLongUrl());
        responseDto.setCreatedAt(url.getCreatedAt());
        return responseDto;
 }
}
