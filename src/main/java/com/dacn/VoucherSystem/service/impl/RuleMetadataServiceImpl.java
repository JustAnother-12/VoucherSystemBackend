package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.RuleMetadataResponse;
import com.dacn.VoucherSystem.dto.response.RuleTypeMetadata;
import com.dacn.VoucherSystem.enums.ConditionType;
import com.dacn.VoucherSystem.enums.RewardType;
import com.dacn.VoucherSystem.service.RuleMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class RuleMetadataServiceImpl implements RuleMetadataService {
    private final ObjectMapper objectMapper;

    public RuleMetadataResponse getMetadata() {
        return new RuleMetadataResponse(
                1,
                Arrays.stream(ConditionType.values())
                        .map(this::getConditionMetadata)
                        .toList(),
                Arrays.stream(RewardType.values())
                        .map(this::getRewardMetadata)
                        .toList()
        );
    }

    private RuleTypeMetadata getConditionMetadata(ConditionType type) {
        return loadMetadata("conditions", type.name());
    }

    private RuleTypeMetadata getRewardMetadata(RewardType type) {
        return loadMetadata("rewards", type.name());
    }

    private RuleTypeMetadata loadMetadata(
            String category,
            String type) {

        String path = "data/rule_metadata/"
                + category + "/"
                + type.toLowerCase() + ".json";

        ClassPathResource resource = new ClassPathResource(path);

        try (InputStream input = resource.getInputStream()) {

            RuleTypeMetadata metadata = objectMapper.readValue(
                    input,
                    RuleTypeMetadata.class
            );

            if (metadata == null
                    || !type.equals(metadata.type())) {
                throw new IllegalStateException(
                        "Metadata type does not match file: " + path
                );
            }

            return metadata;

        } catch (IOException | JacksonException ex) {
            throw new IllegalStateException(
                    "Cannot load rule metadata: " + path,
                    ex
            );
        }
    }
}
