package com.cruz.url_shortener.service;

import com.cruz.url_shortener.dto.LoginRequestDto;
import com.cruz.url_shortener.dto.LoginResponseDto;
import com.cruz.url_shortener.dto.RegistrationRequestDto;
import com.cruz.url_shortener.dto.RegistrationResponseDto;
import org.springframework.http.ResponseCookie;

import java.util.UUID;

public interface AuthService
{
    RegistrationResponseDto registerUser(RegistrationRequestDto registrationRequestDto);
    LoginResponseDto login(String sessionIdCookie, LoginRequestDto loginRequestDto);
}
