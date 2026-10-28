package com.dacn.VoucherSystem;

import com.dacn.VoucherSystem.dto.request.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class TestDataHelper {

    public static LoginRequest createSuccessLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("system_admin");
        request.setPassword("SystemAdmin123");
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
        request.setUsername("system_backup_admin");
        request.setPassword("SystemAdminB123");
        return request;
    }



}
