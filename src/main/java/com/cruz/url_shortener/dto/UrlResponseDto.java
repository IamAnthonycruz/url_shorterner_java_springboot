package com.cruz.url_shortener.dto;


import lombok.Data;

import java.time.Instant;

@Data
public class UrlResponseDto {
    String shortUrl;
    String longUrl;
    Instant createdAt;
}
