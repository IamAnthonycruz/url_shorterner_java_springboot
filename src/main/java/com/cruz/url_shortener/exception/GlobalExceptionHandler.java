package com.cruz.url_shortener.exception;

import com.cruz.url_shortener.dto.ErrorResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(MethodArgumentNotValidException ex){
        var errorResponseDto = new ErrorResponseDto();
        ArrayList<String> errors = new ArrayList<String>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.add(error.getDefaultMessage()));
        errorResponseDto.setStatus(400);
        errorResponseDto.setMessage("Bad Request");
        errorResponseDto.setErrors(errors);
        return ResponseEntity.status(400).body(errorResponseDto);
    }
    @ExceptionHandler(ShortCodeNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleShortCodeNotFoundException(ShortCodeNotFoundException ex) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto();
        ArrayList<String> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        errorResponseDto.setStatus(404);
        errorResponseDto.setErrors(errors);
        errorResponseDto.setMessage("Not Found");
        return ResponseEntity.status(404).body(errorResponseDto);
    }
    @ExceptionHandler(UserAlreadyCreatedException.class)
    public ResponseEntity<ErrorResponseDto> handleUserAlreadyCreatedException(UserAlreadyCreatedException ex){
        ErrorResponseDto errorResponseDto = new ErrorResponseDto();
        ArrayList<String> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        errorResponseDto.setStatus(429);
        errorResponseDto.setErrors(errors);
        errorResponseDto.setMessage("Conflict");
        return ResponseEntity.status(409).body(errorResponseDto);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> exception(Exception ex){
            ErrorResponseDto errorResponseDto = new ErrorResponseDto();
            ArrayList<String> errors = new ArrayList<>();
            errors.add(ex.getMessage());
            errorResponseDto.setStatus(500);
            errorResponseDto.setErrors(errors);
            errorResponseDto.setMessage("Internal Server Error");
            return ResponseEntity.status(500).body(errorResponseDto);
    }
    @ExceptionHandler(InvalidCredentialException.class)
    public ResponseEntity<ErrorResponseDto> errorResponseDto(InvalidCredentialException ex){
        ErrorResponseDto errorResponseDto = new ErrorResponseDto();
        ArrayList<String> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        errorResponseDto.setStatus(401);
        errorResponseDto.setErrors(errors);
        errorResponseDto.setMessage("Username or Password is Invalid");
        return ResponseEntity.status(401).body(errorResponseDto);
    }


}
