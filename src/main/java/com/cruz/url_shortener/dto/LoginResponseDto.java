package com.cruz.url_shortener.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginResponseDto {
    String userName;
    public LoginResponseDto(String userName){
        this.userName = userName;
    }

}
