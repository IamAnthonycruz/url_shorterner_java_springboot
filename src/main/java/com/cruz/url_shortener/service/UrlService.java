package com.cruz.url_shortener.service;

import com.cruz.url_shortener.dto.UrlRequestDto;
import com.cruz.url_shortener.dto.UrlResponseDto;
import com.cruz.url_shortener.entity.User;

public interface UrlService {
    UrlResponseDto shortenUrl(UrlRequestDto urlRequestDto, Long userId);
    String getLongUrl(String shortCode);
    void disableShortUrl(String shortCode);
}
