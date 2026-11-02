package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.request.BlackListCreateRequest;
import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.BlackListResponse;
import com.dacn.VoucherSystem.service.interfaces.BlackListService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/black-lists")
@Tag(name = "Fraud & Security", description = "Operations pertaining to Fraud logs and Black lists")
public class BlackListController {
    private final BlackListService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BlackListResponse>>> getAllBlackLists() {
        List<BlackListResponse> blackLists = service.getAllBlackList();

        ApiResponse<List<BlackListResponse>> response = ApiResponse.<List<BlackListResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get black lists successfully!")
                .data(blackLists)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BlackListResponse>> getBlackListById(@PathVariable Long id) {
        BlackListResponse blackList = service.getBlackListById(id);

        ApiResponse<BlackListResponse> response = ApiResponse.<BlackListResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get black list successfully!")
                .data(blackList)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BlackListResponse>> create(
            @Valid @RequestBody BlackListCreateRequest request
    ) {
        BlackListResponse result = service.createBlackList(
                request.blacklistType(),
                request.targetValue(),
                request.reason(),
                request.expiredAt()
        );

        ApiResponse<BlackListResponse> response = ApiResponse.<BlackListResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Black list created successfully")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<BlackListResponse>> deactivateBlackList(@PathVariable long id) {
        BlackListResponse result = service.deactivateBlackList(id);

        ApiResponse<BlackListResponse> response = ApiResponse.<BlackListResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Black list deactivate successfully!")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }
}
