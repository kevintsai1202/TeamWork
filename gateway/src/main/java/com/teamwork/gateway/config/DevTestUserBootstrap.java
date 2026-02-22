package com.teamwork.gateway.config;

import com.teamwork.gateway.entity.UserAccount;
import com.teamwork.gateway.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(1)
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class DevTestUserBootstrap implements ApplicationRunner {

    private final UserAccountRepository userAccountRepository;

    /**
     * 初始化 dev/test 測試用使用者投影資料。
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUser("u_alice", "tenant-dev", "alice", "Alice", "ACTIVE");
        seedUser("u_bob", "tenant-dev", "bob", "Bob", "ACTIVE");
        log.info("Dev/Test users bootstrap completed.");
    }

    private void seedUser(String id, String tenantId, String username, String displayName, String status) {
        if (userAccountRepository.existsById(id)) {
            return;
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setId(id);
        userAccount.setTenantId(tenantId);
        userAccount.setUsername(username);
        userAccount.setDisplayName(displayName);
        userAccount.setStatus(status);
        userAccountRepository.save(userAccount);
    }
}