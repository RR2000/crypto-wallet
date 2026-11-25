package com.rondinella.authorizationserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import com.rondinella.authorizationserver.usermanaging.UserRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AuthorizationServerApplicationTests {

  @Autowired MockMvc mvc;
  @Autowired CryptoUserDetailsService users;
  @Autowired UserRepository repository;

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
  }
}
