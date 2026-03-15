package com.cruz.url_shortener.dto;

import lombok.Data;

import java.time.Instant;
@Data
public class RegistrationResponseDto {
    private String userName;
    private Instant createdAt;
}
