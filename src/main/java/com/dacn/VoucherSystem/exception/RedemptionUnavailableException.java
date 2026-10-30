package com.dacn.VoucherSystem.exception;

public class RedemptionUnavailableException extends RuntimeException {
    public RedemptionUnavailableException(String message, Throwable cause) {
        super(message);
    }
}
