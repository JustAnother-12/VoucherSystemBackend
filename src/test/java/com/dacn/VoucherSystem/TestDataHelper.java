package com.dacn.VoucherSystem;

import com.dacn.VoucherSystem.dto.request.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class TestDataHelper {

    public static LoginRequest createSuccessLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("superadmin");
        request.setPassword("SuperAdmin123");
        return request;
    }

    public static LoginRequest createLoginBadRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("");
        request.setPassword("");
        return request;
    }

    public static LoginRequest createInActiveLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("marketing_lead_inactive");
        request.setPassword("FinanceLeadInActive123");
        return request;
    }



}
