package com.cruz.url_shortener.service;

import com.cruz.url_shortener.dtos.urlRequestDto;
import com.cruz.url_shortener.dtos.urlResponseDto;

public interface urlService {
    urlResponseDto shortenUrl(urlRequestDto urlRequestDto);

}
