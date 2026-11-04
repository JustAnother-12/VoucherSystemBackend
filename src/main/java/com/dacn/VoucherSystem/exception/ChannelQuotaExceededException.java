package com.dacn.VoucherSystem.exception;

public class ChannelQuotaExceededException extends RuntimeException {
    public ChannelQuotaExceededException(String message) {
        super(message);
    }
}
