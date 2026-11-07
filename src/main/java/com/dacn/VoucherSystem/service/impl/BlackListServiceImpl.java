package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.BlackListResponse;
import com.dacn.VoucherSystem.entity.BlackList;
import com.dacn.VoucherSystem.enums.BlacklistType;
import com.dacn.VoucherSystem.event.BlackListChangedEvent;
import com.dacn.VoucherSystem.exception.ResourceAlreadyExistsException;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.mapper.BlackListMapper;
import com.dacn.VoucherSystem.repository.BlackListRepository;
import com.dacn.VoucherSystem.service.interfaces.BlackListService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlackListServiceImpl implements BlackListService {
    private final BlackListRepository blackListRepository;
    private final BlackListMapper blackListMapper;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<BlackListResponse> getAllBlackList() {
        List<BlackList> configs = blackListRepository.findAll();

        return configs.stream()
                .map(blackListMapper::toBlackListResponse)
                .toList();
    }

    @Override
    public BlackListResponse getBlackListById(Long id) {
        BlackList config = blackListRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No black list with id " + id + " found in the system")
                );

        return blackListMapper.toBlackListResponse(config);
    }

    @Override
    @Transactional
    public BlackListResponse createBlackList(
            BlacklistType blacklistType,
            String targetValue,
            String reason,
            LocalDateTime expiredAt
    ) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (expiredAt != null
                && !expiredAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Blacklist expiration must be in the future"
            );
        }

        boolean existing = blackListRepository.existsActive(
                blacklistType,
                targetValue.trim(),
                now
        );

        if (existing) {
            throw new ResourceAlreadyExistsException(
                    "An active blacklist already exists for this target"
            );
        }

        BlackList blackList = BlackList.builder()
                .createdAt(LocalDateTime.now(clock))
                .blacklistType(blacklistType)
                .targetValue(targetValue.trim())
                .reason(reason.trim())
                .isActive(true)
                .expiredAt(expiredAt)
                .build();

        BlackList saved = blackListRepository.saveAndFlush(blackList);

        eventPublisher.publishEvent(
                new BlackListChangedEvent(
                        saved.getBlacklistType(),
                        saved.getTargetValue()
                )
        );

        return blackListMapper.toBlackListResponse(blackList);
    }

    @Override
    @Transactional
    public BlackListResponse deactivateBlackList(Long id) {
        BlackList blackList = blackListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Blacklist not found: " + id
                ));

        blackList.setIsActive(false);

        BlackList saved = blackListRepository.save(blackList);

        eventPublisher.publishEvent(
                new BlackListChangedEvent(
                        saved.getBlacklistType(),
                        saved.getTargetValue()
                )
        );

        return blackListMapper.toBlackListResponse(saved);
    }
}
