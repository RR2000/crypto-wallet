package com.rondinella.authorizationserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rondinella.authorizationserver.github.GitHubUserService;
import com.rondinella.authorizationserver.github.GitHubUserService.GitHubEmail;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDatabase;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import com.rondinella.authorizationserver.usermanaging.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
    "wallet.github.client-id=test-client-id",
    "wallet.github.client-secret=test-client-secret"})
@AutoConfigureMockMvc
class AuthorizationServerApplicationTests {

  @Autowired MockMvc mvc;
  @Autowired CryptoUserDetailsService users;
  @Autowired UserRepository repository;
  @Autowired GitHubUserService gitHubUsers;

  private MockHttpServletRequestBuilder register(String email, String password) {
    return post("/addUser").with(csrf())
        .param("email", email).param("password", password)
        .param("firstName", "Ada").param("lastName", "Lovelace");
  }

  private static SimpleGrantedAuthority walletScope() {
    return new SimpleGrantedAuthority("SCOPE_wallet");
  }

  @Test
  void publishesTheOpenIdConfiguration() throws Exception {
    mvc.perform(get("/.well-known/openid-configuration"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userinfo_endpoint").exists())
        .andExpect(jsonPath("$.end_session_endpoint").exists());
  }

  @Test
  void registersAUserAndNeverOverwritesAnExistingOne() throws Exception {
    mvc.perform(register("ada@example.com", "first-password"))
        .andExpect(redirectedUrl("/login?registered"));
    assertThat(users.passwordMatches("ada@example.com", "first-password")).isTrue();

    mvc.perform(register("ADA@example.com", "second-password"))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("error"));
    assertThat(users.passwordMatches("ada@example.com", "first-password")).isTrue();
  }

  @Test
  void rejectsShortPasswords() throws Exception {
    mvc.perform(register("short@example.com", "1234567"))
        .andExpect(model().attributeExists("error"));
    assertThat(repository.existsById("short@example.com")).isFalse();
  }

  @Test
  void theApiNeedsATokenWithTheWalletScope() throws Exception {
    mvc.perform(post("/api/password/verify").param("password", "x")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/password/verify").param("password", "x").with(jwt().jwt(j -> j.subject("nobody@example.com"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void verifiesAndChangesThePasswordAndStoresTheWallet() throws Exception {
    mvc.perform(register("bob@example.com", "old-password"));
    var bob = jwt().jwt(j -> j.subject("bob@example.com")).authorities(walletScope());

    mvc.perform(post("/api/password/verify").param("password", "old-password").with(bob)).andExpect(status().isNoContent());
    mvc.perform(post("/api/password/verify").param("password", "wrong-password").with(bob)).andExpect(status().isForbidden());

    mvc.perform(post("/api/password").param("currentPassword", "wrong-password").param("newPassword", "new-password").with(bob))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/password").param("currentPassword", "old-password").param("newPassword", "new-password").with(bob))
        .andExpect(status().isNoContent());
    assertThat(users.passwordMatches("bob@example.com", "new-password")).isTrue();

    mvc.perform(post("/api/wallet").param("wallet", "[[{\"address\":\"abc\"}],[\"Main\"],0]").with(bob))
        .andExpect(status().isNoContent());
    assertThat(repository.findByEmail("bob@example.com").getWallet()).contains("Main");
  }

  @Test
  void theProfileNeverContainsThePasswordHash() throws Exception {
    mvc.perform(register("carol@example.com", "carol-password"));
    var claims = users.loadOidcUserByUsername("carol@example.com", Set.of(OidcScopes.PROFILE, OidcScopes.EMAIL, "wallet")).getClaims();
    assertThat(claims).containsKeys("email", "wallet", "given_name").doesNotContainKey("password");
    assertThat(claims).containsEntry("has_password", true);
  }

  @Test
  void withoutSmtpThereIsNoPasswordResetByEmail() throws Exception {
    mvc.perform(get("/login")).andExpect(model().attribute("passwordReset", false));
    mvc.perform(get("/features")).andExpect(jsonPath("$.passwordReset").value(false));
    mvc.perform(get("/forgotPassword")).andExpect(status().isNotFound());
    mvc.perform(post("/forgotPassword").with(csrf()).param("email", "someone@example.com")).andExpect(status().isNotFound());
    mvc.perform(get("/resetPassword")).andExpect(status().isNotFound());
  }

  @Test
  void theLoginPageOffersGitHubAndItRedirectsThere() throws Exception {
    mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(model().attribute("gitHub", true));
    String location = mvc.perform(get("/oauth2/authorization/github"))
        .andExpect(status().is3xxRedirection())
        .andReturn().getResponse().getRedirectedUrl();
    assertThat(location).startsWith("https://github.com/login/oauth/authorize")
        .contains("client_id=test-client-id", "scope=read:user%20user:email", "/login/oauth2/code/github");
  }

  @Test
  void gitHubLinksTheAccountWithTheSameVerifiedEmail() throws Exception {
    mvc.perform(register("dave@example.com", "dave-password"));
    var linked = gitHubUsers.toLocalUser(List.of(
        new GitHubEmail("dave-work@example.com", true, true),
        new GitHubEmail("DAVE@example.com", false, true)), Map.of("login", "dave", "name", "Dave Hacker"));
    assertThat(linked.getName()).isEqualTo("dave@example.com");
    assertThat(users.passwordMatches("dave@example.com", "dave-password")).isTrue();
    assertThat(repository.existsById("dave-work@example.com")).isFalse();
  }

  @Test
  void gitHubIgnoresUnverifiedEmailsAndCreatesAnAccountWithoutPassword() {
    // someone with an unverified "erin@example.com" on GitHub must not land in erin's account
    users.register(form("erin@example.com", "erin-password"));
    var created = gitHubUsers.toLocalUser(List.of(
        new GitHubEmail("erin@example.com", true, false),
        new GitHubEmail("mallory@example.com", false, true)), Map.of("login", "mallory"));
    assertThat(created.getName()).isEqualTo("mallory@example.com");
    var claims = users.loadOidcUserByUsername("mallory@example.com", Set.of(OidcScopes.PROFILE, "wallet")).getClaims();
    assertThat(claims).containsEntry("has_password", false).containsEntry("given_name", "mallory");

    assertThatThrownBy(() -> gitHubUsers.toLocalUser(List.of(new GitHubEmail("x@example.com", true, false)), Map.of()))
        .isInstanceOf(OAuth2AuthenticationException.class);
  }

  @Test
  void aGitHubAccountChoosesItsFirstPasswordOnce() throws Exception {
    gitHubUsers.toLocalUser(List.of(new GitHubEmail("frank@example.com", true, true)), Map.of("login", "frank"));
    var frank = jwt().jwt(j -> j.subject("frank@example.com")).authorities(walletScope());
    mvc.perform(post("/api/password/verify").param("password", "anything").with(frank)).andExpect(status().isForbidden());
    mvc.perform(post("/api/password/set").param("newPassword", "short").with(frank)).andExpect(status().isBadRequest());
    mvc.perform(post("/api/password/set").param("newPassword", "frank-password").with(frank)).andExpect(status().isNoContent());
    mvc.perform(post("/api/password/set").param("newPassword", "other-password").with(frank)).andExpect(status().isConflict());
    assertThat(users.passwordMatches("frank@example.com", "frank-password")).isTrue();
  }

  @Test
  void aForgottenPasswordIsResetOnlyAfterAGitHubLoginAndTheWalletStays() throws Exception {
    mvc.perform(register("grace@example.com", "grace-password"));
    users.updateCryptoUserWallet("grace@example.com", "[[{\"address\":\"abc\"}],[\"Old\"],0]");
    var withPassword = jwt().jwt(j -> j.subject("grace@example.com").claim("login", "password")).authorities(walletScope());
    var withGitHub = jwt().jwt(j -> j.subject("grace@example.com").claim("login", "github")).authorities(walletScope());

    mvc.perform(post("/api/password/reset").param("newPassword", "new-grace-password").with(withPassword))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/password/reset").param("newPassword", "new-grace-password").with(withGitHub))
        .andExpect(status().isNoContent());
    assertThat(users.passwordMatches("grace@example.com", "new-grace-password")).isTrue();
    assertThat(repository.findByEmail("grace@example.com").getWallet()).contains("Old");
  }

  private static CryptoUserDatabase form(String email, String password) {
    CryptoUserDatabase f = new CryptoUserDatabase();
    f.setEmail(email);
    f.setPassword(password);
    f.setFirstName("Test");
    f.setLastName("User");
    return f;
  }
}
