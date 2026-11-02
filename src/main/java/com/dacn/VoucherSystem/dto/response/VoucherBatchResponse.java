package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.enums.BatchStatus;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.DurationUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

import java.util.List;
import lombok.Builder;

@Builder
public record VoucherBatchResponse(

        @Schema(description = "ID định danh duy nhất của lô mã do hệ thống sinh ra")
        Long id,

        @Schema(description = "ID của chiến dịch")
        Long campaignId,

        @Schema(description = "ID của quy tắc áp dụng")
        Long ruleId,

        @Schema(description = "Loại lô mã")
        BatchType batchType,

        @Schema(implementation = CodeConfig.class)
        JsonNode codeConfig,

        @Schema(example = "DAYS")
        DurationUnit durationUnit,

        @Schema(example = "30")
        Integer durationValue,

        @Schema(description = "Số lượng mã đã được hệ thống sinh ra thực tế (do Server tính toán)", example = "1000")
        Integer generatedCount,

        @Schema(description = "Số lượng mã đã được khách hàng sử dụng / quy đổi thành công", example = "150")
        Integer redeemedCount,

        @Schema(implementation = DistributionChannels.class)
        JsonNode distributionChannels,

        BatchStatus status
) {

    public record CodeConfig(String prefix, Integer length, String charset) {}
    public record DistributionChannels(List<DistributionConfig> allocations) {}
    public record DistributionConfig(String channel, Integer quota) {}
}