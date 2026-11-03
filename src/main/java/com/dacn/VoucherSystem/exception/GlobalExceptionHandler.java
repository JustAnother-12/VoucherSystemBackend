package com.dacn.VoucherSystem.exception;

import com.dacn.VoucherSystem.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
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

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceExists(
            ResourceAlreadyExistsException ex
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidDistributionTargetException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidDistributionTargetException(InvalidDistributionTargetException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(NoAvailableVoucherException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoAvailableVoucherException(NoAvailableVoucherException ex) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
    }

    @ExceptionHandler(DistributionSendFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDistributionSendFailedException(DistributionSendFailedException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(DuplicateDistributionException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateDistributionException(DuplicateDistributionException ex) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
    }

    @ExceptionHandler(UnsupportedBatchTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedBatchTypeException(UnsupportedBatchTypeException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(ChannelQuotaExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleChannelQuotaExceededException(ChannelQuotaExceededException ex) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
    }

    @ExceptionHandler(ChannelNotEnabledForBatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleChannelNotEnabledForBatchException(ChannelNotEnabledForBatchException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidCampaignException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCampaignException(InvalidCampaignException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(
            RateLimitExceededException ex
    ) {
        return buildResponse(
                HttpStatus.TOO_MANY_REQUESTS,
                ex.getMessage()
        );
    }

    @ExceptionHandler(RedemptionBlockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedemptionBlocked(
            RedemptionBlockedException ex
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );
    }

    @ExceptionHandler(RedemptionUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedemptionUnavailable(
            RedemptionUnavailableException ex
    ) {
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage()
        );
    }

    @ExceptionHandler(VoucherNotApplicableException.class)
    public ResponseEntity<ApiResponse<Void>> handleVoucherNotApplicable(
            VoucherNotApplicableException ex
    ) {
        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
    }

    @ExceptionHandler(RedemptionConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedemptionConflict(
            RedemptionConflictException ex
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(RuleConfigHasPublishedException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuleConfigPublished(RuleConfigHasPublishedException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
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

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Username or Password is invalid"
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
