package com.dacn.VoucherSystem.exception;

public class ChannelNotEnabledForBatchException extends RuntimeException {
    public ChannelNotEnabledForBatchException(String message) {
        super(message);
    }
}
