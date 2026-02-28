package com.cruz.url_shortener.service;

import com.cruz.url_shortener.dto.urlRequestDto;
import com.cruz.url_shortener.dto.urlResponseDto;

public interface urlService {
    urlResponseDto shortenUrl(urlRequestDto urlRequestDto);

}
