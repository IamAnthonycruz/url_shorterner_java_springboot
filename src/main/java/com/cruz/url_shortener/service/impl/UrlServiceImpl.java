package com.cruz.url_shortener.service.impl;

import com.cruz.url_shortener.component.Base62Encoder;
import com.cruz.url_shortener.config.AppProperties;
import com.cruz.url_shortener.dto.UrlRequestDto;
import com.cruz.url_shortener.dto.UrlResponseDto;
import com.cruz.url_shortener.mapper.UrlMapper;
import com.cruz.url_shortener.repository.UrlRepository;
import com.cruz.url_shortener.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {
    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final UrlMapper urlMapper;
    private final AppProperties appProperties;
    @Override
    public UrlResponseDto shortenUrl(UrlRequestDto urlRequestDto) {
        try {
            var entity = urlRepository.findByLongUrl(urlRequestDto.getLongUrl());
            if (entity.isPresent()) {
                var responseDto = urlMapper.toResponseDto(entity.get());
                responseDto.setShortUrl(appProperties.getBaseUrl()+"/"+entity.get().getShortCode());
                return responseDto;
            } else {
                var urlEntity = urlMapper.toEntity(urlRequestDto);
                urlRepository.save(urlEntity);
                var shortCode = base62Encoder.encode(urlEntity.getId());
                urlEntity.setShortCode(shortCode);
                urlRepository.save(urlEntity);
                var responseDto = urlMapper.toResponseDto(urlEntity);
                responseDto.setShortUrl(appProperties.getBaseUrl()+"/"+urlEntity.getShortCode());
                return responseDto;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    HashMap<String, String> mockRedis = new HashMap<>();

    @Override
    public String getLongUrl(String shortCode) {
        String longUrl;
        var entity = urlRepository.findByShortCode(shortCode);
        if(entity.isEmpty()) {
            throw new RuntimeException();
        }
        if(mockRedis.containsKey(shortCode)) {
            longUrl = mockRedis.get(shortCode);
        }
        else{
            longUrl = entity.get().getLongUrl();
            mockRedis.put(shortCode, longUrl);
        }
        //will update once i have the redis set up and set up the exception
        var hitCount = entity.get().getHitCount();
        hitCount++;
        entity.get().setHitCount(hitCount);
        entity.ifPresent(urlRepository::save);
        return longUrl;
    }
}
