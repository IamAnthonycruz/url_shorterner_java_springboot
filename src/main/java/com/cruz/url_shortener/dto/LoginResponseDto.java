package com.cruz.url_shortener.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class LoginResponseDto {
    private UUID uuid;
    private String token;
}
