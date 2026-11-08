package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.DurationUnit;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.JsonNode;

import java.util.List;

public record VoucherBatchCreateRequest(

        @NotNull Long campaignId,
        @NotNull Long ruleId,
        @NotNull BatchType batchType,

        @NotNull(message = "codeConfig là bắt buộc")
        @Schema(description = "Cấu hình sinh mã", implementation = CodeConfig.class)
        JsonNode codeConfig,

        @NotNull DurationUnit durationUnit,
        @NotNull @Positive Integer durationValue,

        @Positive
        @Schema(description = "Số lần tối đa mã được sử dụng. GENERIC: bắt buộc > 0. BULK: bỏ qua, tự lấy bằng tổng quota", example = "100")
        Integer maxUsageCount,

        @JsonProperty("distribution_channels")
        @Schema(implementation = DistributionChannels.class)
        @NotNull JsonNode distributionChannels
) {
    public record CodeConfig(String prefix, Integer length, String charset) {}
    public record DistributionChannels(List<DistributionConfig> allocations) {}
    public record DistributionConfig(String channel, Integer quota) {}
}