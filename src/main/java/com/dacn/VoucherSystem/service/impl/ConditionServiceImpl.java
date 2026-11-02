package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.ConditionResponse;
import com.dacn.VoucherSystem.entity.Condition;
import com.dacn.VoucherSystem.enums.ConditionType;
import com.dacn.VoucherSystem.enums.RuleStatus;
import com.dacn.VoucherSystem.event.VoucherSnapshotsChangedEvent;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.exception.RuleConfigHasPublishedException;
import com.dacn.VoucherSystem.mapper.ConditionMapper;
import com.dacn.VoucherSystem.repository.ConditionRepository;
import com.dacn.VoucherSystem.service.interfaces.ConditionService;
import com.dacn.VoucherSystem.util.RuleConfigValidator;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConditionServiceImpl implements ConditionService {
    private final ApplicationEventPublisher eventPublisher;
    private final ConditionRepository repository;
    private final ConditionMapper mapper;
    private final RuleConfigValidator validator;

    @Override
    public List<ConditionResponse> getAllCondition() {
        List<Condition> condition = repository.findAll();

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
    @Transactional
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

        if (condition.getStatus().equals(RuleStatus.PUBLISHED))
            throw new RuleConfigHasPublishedException("The condition has been Published");

        if (name.isPresent() && !name.get().isEmpty())
            condition.setConditionName(name.get());

        ConditionType updatedType = type.isPresent()
                ? type.get()
                : condition.getConditionType();

        JsonNode updatedConfig = config.isPresent()
                ? config.get()
                : condition.getConditionJson();

        validator.validateCondition(updatedType, updatedConfig);

        condition.setConditionType(updatedType);
        condition.setConditionJson(updatedConfig);

        repository.save(condition);

        eventPublisher.publishEvent(
                new VoucherSnapshotsChangedEvent()
        );

        return mapper.toConditionResponse(condition);
    }
}
