package com.example.wallet.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApiError {

    private final String code;
    private final String message;
    private final LocalDateTime timestamp;

    private ApiError(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message);
    }
}
