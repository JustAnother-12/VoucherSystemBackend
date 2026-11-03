package com.dacn.VoucherSystem.service.sender;

public interface DistributionSender {

    // (DistributionService.EMAIL or DistributionService.WEBHOOK)
    String channel();

    //Handle InvalidDistributionTargetException if targetAddress invalid with this channel
    void validateTarget(String targetAddress);

    // Handle DistributionSendFailedException if send fail
    void send(DistributionPayload payload);
}