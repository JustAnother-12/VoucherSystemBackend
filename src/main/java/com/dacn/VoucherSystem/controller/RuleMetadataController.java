package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.RuleMetadataResponse;
import com.dacn.VoucherSystem.service.RuleMetadataService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rule/metadata")
@Tag(name = "Rule Engine", description = "Operations pertaining to rules, rewards, and conditions")
public class RuleMetadataController {

    private final RuleMetadataService service;

    @GetMapping
    public ResponseEntity<ApiResponse<RuleMetadataResponse>>
    getMetadata() {

        var data = service.getMetadata();

        var response =
                ApiResponse.<RuleMetadataResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Get rule metadata successfully!")
                        .data(data)
                        .build();

        return ResponseEntity.ok(response);
    }
}
