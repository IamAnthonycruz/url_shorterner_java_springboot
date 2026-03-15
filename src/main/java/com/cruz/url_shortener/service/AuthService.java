package com.cruz.url_shortener.service;

import com.cruz.url_shortener.dto.RegistrationRequestDto;
import com.cruz.url_shortener.dto.RegistrationResponseDto;

public interface AuthService
{
    RegistrationResponseDto registerUser(RegistrationRequestDto registrationRequestDto);
}
