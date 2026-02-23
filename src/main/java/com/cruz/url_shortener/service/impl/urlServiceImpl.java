package com.cruz.url_shortener.service.impl;

import com.cruz.url_shortener.dtos.urlRequestDto;
import com.cruz.url_shortener.dtos.urlResponseDto;
import com.cruz.url_shortener.repository.urlRepository;
import com.cruz.url_shortener.service.urlService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class urlServiceImpl implements urlService {
    private final urlRepository urlRepository;

    @Override
    @Transactional
    public urlResponseDto shortenUrl(urlRequestDto urlRequestDto) {
        var url = urlRepository.findByLongUrl(urlRequestDto.getLongUrl());
        if(url.isPresent()) {
            var shortUrl = url.get().getShortUrl();
            var hitCount = url.get().getHit_count();
            url.get().setHit_count(hitCount+1);
            var responseDto = new urlResponseDto();
            responseDto.setShortUrl(shortUrl);
            return responseDto;
        }
        return null;
    }
}
