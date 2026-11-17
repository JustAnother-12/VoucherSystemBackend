package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.enums.DistributionStatus;
import com.dacn.VoucherSystem.exception.DistributionSendFailedException;
import com.dacn.VoucherSystem.service.sender.DistributionPayload;
import com.dacn.VoucherSystem.service.sender.DistributionSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributionDispatchService {
    private final Map<String, DistributionSender> senders;
    private final DistributionLogStatusUpdater statusUpdater;

    @Async("distributionExecutor")
    public void dispatch(Long logId, String channel, DistributionPayload payload) {
        DistributionStatus status;
        try {
            DistributionSender sender = senders.get(channel);
            if (sender == null) {
                throw new IllegalStateException("No distribution sender configured for channel " + channel);
            }
            sender.send(payload);
            status = DistributionStatus.SENT;
        } catch (DistributionSendFailedException exception) {
            log.warn("Distribution {} via {} failed: {}", logId, channel, exception.getMessage());
            status = DistributionStatus.FAILED;
        }

        statusUpdater.updateStatus(logId, status);
    }
}
