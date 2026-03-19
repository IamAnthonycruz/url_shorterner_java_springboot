package com.cruz.url_shortener.service;

import com.cruz.url_shortener.dto.LoginRequestDto;
import com.cruz.url_shortener.dto.LoginResponseDto;
import com.cruz.url_shortener.dto.RegistrationRequestDto;
import com.cruz.url_shortener.dto.RegistrationResponseDto;

public interface AuthService
{
    RegistrationResponseDto registerUser(RegistrationRequestDto registrationRequestDto);
    LoginResponseDto login(LoginRequestDto loginRequestDto);
}
