package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.DistributionLogResponse;
import com.dacn.VoucherSystem.entity.DistributionLog;
import com.dacn.VoucherSystem.entity.Voucher;
import com.dacn.VoucherSystem.entity.VoucherBatch;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.DistributionStatus;
import com.dacn.VoucherSystem.exception.*;
import com.dacn.VoucherSystem.mapper.DistributionLogMapper;
import com.dacn.VoucherSystem.repository.DistributionLogRepository;
import com.dacn.VoucherSystem.service.interfaces.DistributionLogService;
import com.dacn.VoucherSystem.service.sender.DistributionPayload;
import com.dacn.VoucherSystem.service.sender.DistributionSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributionLogServiceImpl implements DistributionLogService {
    private final DistributionLogRepository repository;
    private final DistributionLogMapper mapper;
    private final Map<String, DistributionSender> senders;

    @Override
    @Transactional(readOnly = true)
    public List<DistributionLogResponse> getAllDistributionLog(
            Long voucherId,
            Long campaignId,
            DistributionStatus status
    ) {
        List<DistributionLog> logs = repository.search(voucherId, campaignId, status);

        return logs.stream()
                .map(mapper::toDistributionLogResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DistributionLogResponse getDistributionLogById(Long id) {
        DistributionLog distributionLog = repository
                .findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No distribution log with id " + id + " found in the system"));

        return mapper.toDistributionLogResponse(distributionLog);
    }


    /**
     * Không dùng @Transactional cho cả method: log PENDING phải được lưu trước khi gọi
     * dịch vụ bên ngoài, và log FAILED vẫn phải được lưu khi gửi lỗi.
     */
    @Override
    public DistributionLogResponse createDistribution(
            Long batchId,
            Long campaignId,
            String channel,
            String targetAddress
    ) {
        String channelName = channel.trim().toUpperCase(Locale.ROOT);
        DistributionSender sender = senders.get(channelName);
        System.out.println("Channel input: " + channel);
        System.out.println("Normalized channel: " + channelName);
        System.out.println("Registered senders: " + senders.keySet());
        if (sender == null) {
            throw new InvalidDistributionTargetException("Unsupported distribution channel: " + channel);
        }

        String target = targetAddress.trim();
        sender.validateTarget(target);

        VoucherBatch batch = repository
                .findBatchInCampaign(batchId, campaignId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("No voucher batch with id " + batchId + " found in campaign " + campaignId ));

        checkChannelAllocation(batch, batchId, channelName);

        Voucher voucher = selectVoucher(batch.getBatchType(), batchId, campaignId, channelName, target);

        DistributionLog distributionLog = repository.saveAndFlush(
                DistributionLog.builder()
                        .campaign(voucher.getCampaign())
                        .voucher(voucher)
                        .channel(channelName)
                        .targetAddress(target)
                        .status(DistributionStatus.PENDING)
                        .sentAt(LocalDateTime.now())
                        .build()
        );

        DistributionPayload payload = new DistributionPayload(
                voucher.getId(),
                voucher.getCode(),
                batchId,
                campaignId,
                voucher.getCampaign().getCampaignName(),
                target
        );

        try {
            sender.send(payload);
            distributionLog.setStatus(DistributionStatus.SENT);
        } catch (DistributionSendFailedException ex) {
            log.warn("Distribution {} via {} failed: {}", distributionLog.getId(), channelName, ex.getMessage());
            distributionLog.setStatus(DistributionStatus.FAILED);
        }

        distributionLog.setSentAt(LocalDateTime.now());
        repository.save(distributionLog);

        return mapper.toDistributionLogResponse(distributionLog);
    }

    /**
     * Kênh phải nằm trong distributionChannelsJson.allocations của batch.
     * BULK: còn quota, quota = số lượt phân phối tối đa (PENDING + SENT) của kênh đó trong batch.
     * GENERIC: không giới hạn số lượt, bỏ qua quota.
     */
    private void checkChannelAllocation(VoucherBatch batch, Long batchId, String channel) {
        for (JsonNode allocation : batch.getDistributionChannelsJson().path("allocations")) {
            if (channel.equalsIgnoreCase(allocation.path("channel").asText())) {
                // GENERIC: một mã dùng chung, gửi không giới hạn nên không áp dụng quota
                if (batch.getBatchType() == BatchType.GENERIC) {
                    return;
                }

                int quota = allocation.path("quota").asInt();
                long used = repository.countActiveByBatchAndChannel(batchId, channel);

                if (used >= quota) {
                    throw new ChannelQuotaExceededException("Channel Quota Exceeded; " + batchId + ", " + channel + ", " + quota);
                }
                return;
            }
        }

        throw new ChannelNotEnabledForBatchException("Channel Not Enabled For Batch: " + batchId + ", " + channel);
    }
    private Voucher selectVoucher(
            BatchType batchType,
            Long batchId,
            Long campaignId,
            String channel,
            String target
    ) {
        return switch (batchType) {
            case BULK -> selectBulkVoucher(batchId, campaignId);
            case GENERIC -> selectGenericVoucher(batchId, campaignId, channel, target);
            default -> throw new UnsupportedBatchTypeException("Unsupported Batch Type: " + batchType);
        };
    }

    /**
     * BULK: mỗi voucher có mã riêng, chỉ gửi cho một người.
     * Lấy voucher đầu tiên chưa có log PENDING/SENT.
     */
    private Voucher selectBulkVoucher(Long batchId, Long campaignId) {
        return repository
                .findAvailableVouchers(batchId, campaignId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new NoAvailableVoucherException("No Available Voucher"));
    }

    /**
     * GENERIC: một mã dùng chung, gửi được cho nhiều người.
     * Luôn dùng lại voucher của batch, chỉ chặn gửi lặp cho cùng địa chỉ trên cùng kênh.
     */
    private Voucher selectGenericVoucher(
            Long batchId,
            Long campaignId,
            String channel,
            String target
    ) {
        Voucher voucher = repository
                .findVouchersOfBatch(batchId, campaignId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new NoAvailableVoucherException("No Available Voucher"));

        if (repository.existsActiveDistribution(voucher.getId(), channel, target)) {
            throw new DuplicateDistributionException("Duplicate address allocation exception" + target);
        }

        return voucher;
    }


}