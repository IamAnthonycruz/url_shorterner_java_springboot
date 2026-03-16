package com.cruz.url_shortener.service.impl;

import com.cruz.url_shortener.dto.RegistrationRequestDto;
import com.cruz.url_shortener.dto.RegistrationResponseDto;
import com.cruz.url_shortener.entity.User;
import com.cruz.url_shortener.exception.UserAlreadyCreatedException;
import com.cruz.url_shortener.mapper.RegistrationMapper;
import com.cruz.url_shortener.repository.UserRepository;
import com.cruz.url_shortener.service.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationMapper registrationMapper;
    @Override
    public RegistrationResponseDto registerUser(RegistrationRequestDto registrationRequestDto) {
        Optional<User> user = userRepository.findByUserName(registrationRequestDto.getUserName());
        if (user.isPresent()) {
            throw new UserAlreadyCreatedException("User already exists");}
            var newUser = registrationMapper.toEntity(registrationRequestDto);
            newUser.setPassword(passwordEncoder.encode(registrationRequestDto.getPassword()));
            userRepository.save(newUser);

            return registrationMapper.toResponseDto(newUser);

        }
    }
