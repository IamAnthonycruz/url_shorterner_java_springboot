package com.cruz.url_shortener.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ErrorResponseDto {
    private LocalDateTime timeStamp;
    private String message;
    private String details;

    public ErrorResponseDto(LocalDateTime timeStamp, String message, String details){
        this.timeStamp = timeStamp;
        this.message = message;
        this.details = details;
    }
}
