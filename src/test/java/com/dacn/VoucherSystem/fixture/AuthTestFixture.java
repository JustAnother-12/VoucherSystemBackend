package com.dacn.VoucherSystem.fixture;

import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.enums.AdminRole;
import com.dacn.VoucherSystem.repository.AdminAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AuthTestFixture {

    private final AdminAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthTestFixture(
            AdminAccountRepository accountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AdminAccount createSuperAdmin() {
        AdminAccount account = new AdminAccount();

        account.setUsername("system_admin");
        account.setHashedPassword(
                passwordEncoder.encode("SystemAdmin123")
        );
        account.setRole(AdminRole.SYSTEM);
        account.setIsActive(true);

        return accountRepository.save(account);
    }

    public AdminAccount createInactiveAccount() {
        AdminAccount account = new AdminAccount();

        account.setUsername("system_backup_admin");
        account.setHashedPassword(
                passwordEncoder.encode("SystemAdminB123")
        );
        account.setRole(AdminRole.SYSTEM);
        account.setIsActive(false);

        return accountRepository.save(account);
    }

    public void deleteAuthTestAccounts() {
        accountRepository.deleteByUsername("system_admin");
        accountRepository.deleteByUsername("system_backup_admin");
    }
}
