package com.runalitycs.report_generator.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        HttpStatus status = ex.getMessage().contains("already exists")
                ? HttpStatus.CONFLICT
                : HttpStatus.NOT_FOUND;

        log.warn("action=handle status={} uri={} message={}", status.value(), request.getRequestURI(), ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", Instant.now(clock));
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", ex.getMessage());

        return ResponseEntity.status(status).body(error);
    }
}