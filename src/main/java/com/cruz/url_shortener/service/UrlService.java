package com.cruz.url_shortener.service;

import com.cruz.url_shortener.dto.UrlRequestDto;
import com.cruz.url_shortener.dto.UrlResponseDto;
import com.cruz.url_shortener.entity.Url;
import com.cruz.url_shortener.entity.User;

import java.util.List;

public interface UrlService {
    UrlResponseDto shortenUrl(UrlRequestDto urlRequestDto, Long userId);
    String getLongUrl(String shortCode);
    List<UrlResponseDto> getAllUserUrls(Long userId);
    void disableShortUrl(String shortCode, Long userId);
}
