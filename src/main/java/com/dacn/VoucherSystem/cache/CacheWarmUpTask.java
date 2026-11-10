package com.dacn.VoucherSystem.cache;

public interface CacheWarmUpTask {
    String getName();
    void warmUp();
}
