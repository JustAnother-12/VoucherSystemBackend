package com.dacn.VoucherSystem.exception;

import com.dacn.VoucherSystem.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ApiResponse<Void>> buildResponse(HttpStatusCode statusCode, String message){
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(statusCode.value())
                .message(message)
                .build();
        return ResponseEntity.status(statusCode).body(response);
    }

    @ExceptionHandler(InvalidRuleConfigException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRuleConfig(InvalidRuleConfigException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    // Handler for ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
    }

    // Handler for InvalidOrExpiredTokenException
    @ExceptionHandler(InvalidOrExpiredTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOrExpiredToken(InvalidOrExpiredTokenException ex) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            Exception ex) {

        String message = "Invalid request";

        if (ex instanceof MethodArgumentNotValidException validationEx) {
            if (validationEx.getBindingResult().getFieldError() != null) {
                message = validationEx.getBindingResult()
                        .getFieldError()
                        .getDefaultMessage();
            }
        }

        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    // Handler for disabled account exceptions
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledException(DisabledException ex) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "The account has been disabled"
        );
    }


    // Generic Handler for exceptions that were not handled
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("Unexpected server error", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred"
        );
    }
}
