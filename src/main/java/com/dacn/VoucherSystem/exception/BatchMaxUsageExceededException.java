package com.dacn.VoucherSystem.exception;

public class BatchMaxUsageExceededException extends RuntimeException {
    public BatchMaxUsageExceededException(String message) {
        super(message);
    }
}
