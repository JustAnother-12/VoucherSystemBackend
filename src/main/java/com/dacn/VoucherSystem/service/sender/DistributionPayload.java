package com.dacn.VoucherSystem.service.sender;

/**
 * Dữ liệu cần thiết để một sender gửi mã voucher đi.
 */
public record DistributionPayload(
        Long voucherId,
        String voucherCode,
        Long batchId,
        Long campaignId,
        String campaignName,
        String targetAddress
) {
}