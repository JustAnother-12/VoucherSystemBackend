package com.dacn.VoucherSystem.exception;

public class NoAvailableVoucherException extends RuntimeException {
    public NoAvailableVoucherException(String message) {
        super(message);
    }
}
