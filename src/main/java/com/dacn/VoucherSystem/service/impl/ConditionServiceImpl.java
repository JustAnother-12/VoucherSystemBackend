package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.ConditionResponse;
import com.dacn.VoucherSystem.entity.Condition;
import com.dacn.VoucherSystem.enums.ConditionType;
import com.dacn.VoucherSystem.enums.RuleStatus;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.mapper.ConditionMapper;
import com.dacn.VoucherSystem.repository.ConditionRepository;
import com.dacn.VoucherSystem.service.ConditionService;
import com.dacn.VoucherSystem.util.RuleConfigValidator;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConditionServiceImpl implements ConditionService {
    private final ConditionRepository repository;
    private final ConditionMapper mapper;
    private final RuleConfigValidator validator;

    @Override
    public List<ConditionResponse> getAllCondition() {
        List<Condition> condition = repository.findAll();
        if(condition.isEmpty())
            throw new ResourceNotFoundException("No condition found in the system");

        return condition.stream()
                .map(mapper::toConditionResponse)
                .toList();
    }

    @Override
    public ConditionResponse getConditionById(Long id) {
        Condition condition = repository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No condition with id " + id + " found in the system")
                );

        return mapper.toConditionResponse(condition);
    }

    @Override
    public ConditionResponse createCondition(
            String name,
            ConditionType type,
            JsonNode config
    ) {

        validator.validateCondition(type, config);

        Condition condition =
                Condition.builder()
                        .conditionName(name)
                        .conditionType(type)
                        .conditionJson(config)
                        .build();

        condition.setStatus(RuleStatus.DRAFT);

        repository.saveAndFlush(condition);

        return mapper.toConditionResponse(condition);
    }

    @Override
    public ConditionResponse updateCondition(
            Long id,
            JsonNullable<String> name,
            JsonNullable<ConditionType> type,
            JsonNullable<JsonNode> config
    ) {
        Condition condition = repository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No condition with id " + id + " found in the system")
                );

        if (name.isPresent())
            condition.setConditionName(name.get());
        if (type.isPresent())
            condition.setConditionType(type.get());
        if (config.isPresent()) {
            validator.validateCondition(condition.getConditionType(), config.get());
            condition.setConditionJson(config.get());
        }

        repository.save(condition);

        return mapper.toConditionResponse(condition);
    }
}
