package com.dacn.VoucherSystem.service.interfaces;

import com.dacn.VoucherSystem.dto.response.VoucherBatchResponse;
import com.dacn.VoucherSystem.dto.response.VoucherRedeemResponse;
import com.dacn.VoucherSystem.entity.VoucherBatch;
import com.dacn.VoucherSystem.enums.BatchStatus;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.DurationUnit;
import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.JsonNode;

import java.util.List;

public interface VoucherBatchService {
    List<VoucherBatchResponse> getAllVoucherBatches();
    VoucherBatchResponse getVoucherBatchByID(Long id);
    VoucherBatchResponse createVoucherBatch(
            Long campaignId,
            Long ruleId,
            BatchType batchType,
            JsonNode codeConfig,
            DurationUnit durationUnit,
            Integer durationValue,
            Integer maxUsageCount,
            JsonNode distributionChannels
    );
    VoucherBatchResponse updateVoucherBatch(
            Long id,
            JsonNullable<Long> ruleId,
            JsonNullable<BatchType> batchType,
            JsonNullable<JsonNode> codeConfig,
            JsonNullable<DurationUnit> durationUnit,
            JsonNullable<Integer> durationValue,
            JsonNullable<Integer> maxUsageCount,
            JsonNullable<JsonNode> distributionChannels,
            JsonNullable<BatchStatus> status
    );

}
