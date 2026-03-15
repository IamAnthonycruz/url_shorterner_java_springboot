package com.cruz.url_shortener.mapper;

import com.cruz.url_shortener.dto.RegistrationRequestDto;
import com.cruz.url_shortener.dto.RegistrationResponseDto;
import com.cruz.url_shortener.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class RegistrationMapper implements EntityMapper<User, RegistrationRequestDto, RegistrationResponseDto> {

    @Override
    public User toEntity(RegistrationRequestDto requestDto) {
        if(requestDto == null){
            return null;
        }
        User user = new User();
        user.setUserName(requestDto.getUserName());
        return user;
    }

    @Override
    public RegistrationResponseDto toResponseDto(User entity) {
        if(entity == null){
            return null;
        }
        RegistrationResponseDto responseDto = new RegistrationResponseDto();
        responseDto.setUserName(entity.getUserName());
        responseDto.setCreatedAt(entity.getCreatedAt());
        return responseDto;
    }
}
