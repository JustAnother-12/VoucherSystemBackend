package com.dacn.VoucherSystem;

import com.dacn.VoucherSystem.dto.request.LoginRequest;
import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.enums.AdminRole;
import com.dacn.VoucherSystem.repository.AdminAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TestDataHelper {
    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public void setupAuthTestData() {
        adminAccountRepository.deleteAll();

        AdminAccount admin1 = AdminAccount.builder()
                .username("superadmin")
                .hashedPassword(passwordEncoder.encode("SuperAdmin123"))
                .isActive(true)
                .role(AdminRole.SYSTEM)
                .build();


        AdminAccount admin2 = AdminAccount.builder()
                .username("marketing_lead_inactive")
                .hashedPassword(passwordEncoder.encode("FinanceLeadInActive123"))
                .isActive(false)
                .role(AdminRole.FINANCE_MANAGER)
                .build();

        adminAccountRepository.saveAll(List.of(admin1, admin2));
    }

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
