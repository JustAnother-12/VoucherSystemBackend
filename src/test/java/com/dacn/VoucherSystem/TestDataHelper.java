package com.dacn.VoucherSystem;

import com.dacn.VoucherSystem.dto.request.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class TestDataHelper {

    public static LoginRequest createSuccessLoginRequest() {
        return new LoginRequest(
                "system_admin",
                "SystemAdmin123"
        );
    }

    public static LoginRequest createLoginBadRequest() {
        return new LoginRequest(
                "",
                ""
        );
    }

    public static LoginRequest createInActiveLoginRequest() {
        return new LoginRequest(
                "system_backup_admin",
                "SystemAdminB123"
        );
    }



}
