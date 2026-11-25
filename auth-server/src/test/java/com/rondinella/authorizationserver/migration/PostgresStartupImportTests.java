package com.rondinella.authorizationserver.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.rondinella.authorizationserver.github.GitHubUserService;
import com.rondinella.authorizationserver.github.GitHubUserService.GitHubEmail;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDatabase;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import com.rondinella.authorizationserver.usermanaging.UserRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The production setup: the server on PostgreSQL, started with WALLET_IMPORT_FROM pointing at the old H2 file.
 * The users of the old database are there as soon as the application is up, and everything keeps working.
 */
@SpringBootTest
class PostgresStartupImportTests {

  static EmbeddedPostgres postgres;

  @DynamicPropertySource
  static void databases(DynamicPropertyRegistry registry) throws Exception {
    postgres = EmbeddedPostgres.start();
    Path dir = Files.createTempDirectory("old-wallet");
    var old = OldWalletDatabase.create(dir);
    registry.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
    registry.add("spring.datasource.username", () -> "postgres");
    registry.add("spring.datasource.password", () -> "");
    registry.add("wallet.import.from", () -> old.url);
  }

  @AfterAll
  static void stop() throws IOException {
    postgres.close();
  }

  @Autowired CryptoUserDetailsService users;
  @Autowired UserRepository repository;
  @Autowired GitHubUserService gitHubUsers;

  @Test
  void theOldUsersAreThereAndTheServerWorksOnPostgres() {
    assertThat(repository.count()).isEqualTo(3);
    assertThat(users.passwordMatches("ada@example.com", OldWalletDatabase.PASSWORD)).isTrue();
    assertThat(repository.findByEmail("ada@example.com").getWallet()).isEqualTo(OldWalletDatabase.WALLET);
    assertThat(users.loadUserByUsername("ada@example.com").getAuthorities())
        .extracting(Object::toString).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    assertThat(users.loadUserByUsername("old@example.com").isEnabled()).isFalse();
    assertThat(users.passwordMatches("octo@example.com", "anything")).isFalse();

    // new registrations, and GitHub accounts without a password (V2 on PostgreSQL)
    CryptoUserDatabase form = new CryptoUserDatabase();
    form.setEmail("new@example.com");
    form.setPassword("new-password");
    assertThat(users.register(form)).isNull();
    assertThat(users.passwordMatches("new@example.com", "new-password")).isTrue();
    gitHubUsers.toLocalUser(List.of(new GitHubEmail("hub@example.com", true, true)), Map.of("login", "hub"));
    assertThat(repository.findByEmail("hub@example.com").getPassword()).isNull();
    assertThat(repository.count()).isEqualTo(5);
  }
}
