package com.cruz.url_shortener.service;

import com.cruz.url_shortener.dto.UrlRequestDto;
import com.cruz.url_shortener.dto.UrlResponseDto;

public interface UrlService {
    UrlResponseDto shortenUrl(UrlRequestDto urlRequestDto);

}
