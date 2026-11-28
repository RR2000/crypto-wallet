package com.rondinella.authorizationserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rondinella.authorizationserver.github.GitHubUserService;
import com.rondinella.authorizationserver.github.GitHubUserService.GitHubEmail;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDatabase;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import com.rondinella.authorizationserver.usermanaging.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The first access of an account and the archive (the stored wallet) when there is nothing in it yet:
 * an empty wallet, no name, no password, or a token of an account that is not there any more.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:first-access;DB_CLOSE_DELAY=-1",
    "wallet.github.client-id=test-client-id",
    "wallet.github.client-secret=test-client-secret"})
@AutoConfigureMockMvc
class FirstAccessTests {

  private static final String EMPTY_WALLET = "[[],[],[]]";
  private static final Set<String> ALL_SCOPES = Set.of(OidcScopes.PROFILE, OidcScopes.EMAIL, "wallet");

  @Autowired MockMvc mvc;
  @Autowired CryptoUserDetailsService users;
  @Autowired UserRepository repository;
  @Autowired GitHubUserService gitHubUsers;

  @Test
  void aNewAccountStartsWithAnEmptyWalletAndNoName() {
    CryptoUserDatabase form = new CryptoUserDatabase();
    form.setEmail("  New.User@Example.com ");
    form.setPassword("new-user-password");
    // first and last name are optional on the registration page
    assertThat(users.register(form)).isNull();

    var claims = users.loadOidcUserByUsername("new.user@example.com", ALL_SCOPES).getClaims();
    assertThat(claims).containsEntry("wallet", EMPTY_WALLET).containsEntry("has_password", true);
    assertThat(claims.get("name")).isEqualTo("");
    // no value: absent or null, the web app reads them as empty
    assertThat(claims.get("given_name")).isNull();
    assertThat(claims.get("family_name")).isNull();
  }

  @Test
  void aNewGitHubAccountWithoutANameStartsWithAnEmptyWalletAndNoPassword() {
    gitHubUsers.toLocalUser(List.of(new GitHubEmail("octo@example.com", true, true)), Map.of());
    var claims = users.loadOidcUserByUsername("octo@example.com", ALL_SCOPES).getClaims();
    assertThat(claims).containsEntry("wallet", EMPTY_WALLET).containsEntry("has_password", false);
  }

  @Test
  void onlyTheOpenIdScopeGivesJustTheSubject() {
    users.register(account("bare@example.com"));
    var claims = users.loadOidcUserByUsername("bare@example.com", Set.of()).getClaims();
    assertThat(claims).containsOnlyKeys("sub");
  }

  @Test
  void theFirstWalletIsStoredAndAnEmptyOneAgainAfterTheLastAccountIsDeleted() throws Exception {
    users.register(account("first@example.com"));
    var token = walletToken("first@example.com");
    mvc.perform(post("/api/wallet").param("wallet", "[[{\"address\":\"abc\"}],[\"Account 1\"],0]").with(token))
        .andExpect(status().isNoContent());
    mvc.perform(post("/api/wallet").param("wallet", "[[],[],0]").with(token)).andExpect(status().isNoContent());
    assertThat(repository.findByEmail("first@example.com").getWallet()).isEqualTo("[[],[],0]");
  }

  @Test
  void aTokenOfAnAccountThatIsNotThereAnyMoreIsUnauthorizedNotAnError() throws Exception {
    // e.g. test accounts cleaned up while a browser still holds a token
    var gone = walletToken("gone@example.com");
    mvc.perform(post("/api/wallet").param("wallet", EMPTY_WALLET).with(gone)).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/password/verify").param("password", "whatever-password").with(gone))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/password/set").param("newPassword", "whatever-password").with(gone))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/password").param("currentPassword", "x").param("newPassword", "whatever-password").with(gone))
        .andExpect(status().isUnauthorized());
    var goneFromGitHub = jwt().jwt(j -> j.subject("gone@example.com").claim("login", "github"))
        .authorities(new SimpleGrantedAuthority("SCOPE_wallet"));
    mvc.perform(post("/api/password/reset").param("newPassword", "whatever-password").with(goneFromGitHub))
        .andExpect(status().isUnauthorized());
    assertThat(repository.existsById("gone@example.com")).isFalse();
  }

  @Test
  void aMissingParameterIsABadRequest() throws Exception {
    users.register(account("params@example.com"));
    mvc.perform(post("/api/wallet").with(walletToken("params@example.com"))).andExpect(status().isBadRequest());
  }

  private static CryptoUserDatabase account(String email) {
    CryptoUserDatabase f = new CryptoUserDatabase();
    f.setEmail(email);
    f.setPassword("a-long-password");
    return f;
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor walletToken(String email) {
    return jwt().jwt(j -> j.subject(email)).authorities(new SimpleGrantedAuthority("SCOPE_wallet"));
  }
}
