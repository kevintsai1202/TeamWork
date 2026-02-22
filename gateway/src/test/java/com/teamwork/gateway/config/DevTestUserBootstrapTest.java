package com.teamwork.gateway.config;

import com.teamwork.gateway.entity.UserAccount;
import com.teamwork.gateway.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class DevTestUserBootstrapTest {

    @Test
    void run_ShouldSeedDefaultUsersWhenNotExists() throws Exception {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        when(repository.existsById("u_alice")).thenReturn(false);
        when(repository.existsById("u_bob")).thenReturn(false);

        DevTestUserBootstrap bootstrap = new DevTestUserBootstrap(repository);

        bootstrap.run(null);

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(UserAccount::getId)
                .containsExactlyInAnyOrder("u_alice", "u_bob");
    }

    @Test
    void run_ShouldSkipExistingUsers() throws Exception {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        when(repository.existsById("u_alice")).thenReturn(true);
        when(repository.existsById("u_bob")).thenReturn(true);

        DevTestUserBootstrap bootstrap = new DevTestUserBootstrap(repository);

        bootstrap.run(null);

        verify(repository, times(0)).save(org.mockito.ArgumentMatchers.any(UserAccount.class));
    }
}
