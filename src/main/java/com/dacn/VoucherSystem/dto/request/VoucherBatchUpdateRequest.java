package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.BatchStatus;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.DurationUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Builder
public record VoucherBatchUpdateRequest(

        JsonNullable<Long> ruleId,

        JsonNullable<BatchType> batchType,

        @Schema(implementation = CodeConfig.class)
        JsonNullable<JsonNode> codeConfig,

        JsonNullable<DurationUnit> durationUnit,

        JsonNullable<Integer> durationValue,

        @Schema(description = "Số lần tối đa mã được sử dụng. Chỉ có tác dụng với GENERIC")
        JsonNullable<Integer> maxUsageCount,

        @Schema(implementation = DistributionChannels.class)
        JsonNullable<JsonNode> distributionChannels,

        JsonNullable<BatchStatus> status
) {
    public record CodeConfig(String prefix, Integer length, String charset) {}
    public record DistributionChannels(List<DistributionConfig> allocations) {}
    public record DistributionConfig(String channel, @PositiveOrZero Integer quota) {}
}
