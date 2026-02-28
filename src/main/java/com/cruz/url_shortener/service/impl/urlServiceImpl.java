package com.cruz.url_shortener.service.impl;

import com.cruz.url_shortener.dto.urlRequestDto;
import com.cruz.url_shortener.dto.urlResponseDto;
import com.cruz.url_shortener.repository.urlRepository;
import com.cruz.url_shortener.service.urlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class urlServiceImpl implements urlService {
    private final urlRepository urlRepository;


    @Override
    public urlResponseDto shortenUrl(urlRequestDto urlRequestDto) {
        return null;
    }
}
