package com.rondinella.authorizationserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rondinella.authorizationserver.mail.Mailer;
import com.rondinella.authorizationserver.mail.PasswordResetService;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDatabase;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** "Forgot password" by email, with the mail sender mocked: no mail leaves the tests. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:reset;DB_CLOSE_DELAY=-1",
    "wallet.web-url=https://wallet.example.org",
    "wallet.mail.host=smtp.example.org", "wallet.mail.username=user", "wallet.mail.password=secret",
    "wallet.mail.from=noreply@example.org"})
@AutoConfigureMockMvc
class PasswordResetTests {

  private static final Pattern LINK =
      Pattern.compile("https://wallet\\.example\\.org/auth/resetPassword#token=([A-Za-z0-9_-]{43})");
  private static final String WALLET = "[[{\"address\":\"abc\",\"crypto\":{}}],[\"main\"],0]";

  @Autowired MockMvc mvc;
  @Autowired CryptoUserDetailsService users;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean Mailer mailer;

  private static String email() {
    return "r" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
  }

  /** A different client address for every test: the per-address limit does not leak between tests. */
  private static String ip() {
    ThreadLocalRandom r = ThreadLocalRandom.current();
    return "10.2." + r.nextInt(250) + "." + r.nextInt(250);
  }

  private String account() {
    String email = email();
    CryptoUserDatabase form = new CryptoUserDatabase();
    form.setEmail(email);
    form.setPassword("old-password");
    form.setFirstName("Ada");
    form.setLastName("Lovelace");
    assertThat(users.register(form)).isNull();
    users.updateCryptoUserWallet(email, WALLET);
    return email;
  }

  private ResultActions forgot(String email, String ip) throws Exception {
    return mvc.perform(post("/forgotPassword").with(csrf()).header("X-Real-IP", ip).param("email", email));
  }

  private ResultActions reset(String token, String password) throws Exception {
    return mvc.perform(post("/resetPassword").with(csrf())
        .param("token", token).param("password", password).param("confirm", password));
  }

  private String mailedToken(String to) {
    ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
    verify(mailer, timeout(5000)).send(eq(to), anyString(), text.capture());
    Matcher m = LINK.matcher(text.getValue());
    assertThat(m.find()).as("link in the mail").isTrue();
    return m.group(1);
  }

  @Test
  void theLoginPageOffersItAndTheWebAppIsTold() throws Exception {
    mvc.perform(get("/login")).andExpect(content().string(Matchers.containsString("Forgot your password?")));
    mvc.perform(get("/features")).andExpect(jsonPath("$.passwordReset").value(true));
    mvc.perform(get("/forgotPassword")).andExpect(status().isOk());
    mvc.perform(get("/resetPassword"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString("stay in the wallet, locked")));
  }

  @Test
  void sameAnswerWhetherTheAccountExistsOrNot() throws Exception {
    String email = account();
    String nobody = email();
    MvcResult known = forgot(email, ip()).andExpect(redirectedUrl("/forgotPassword?sent")).andReturn();
    MvcResult unknown = forgot(nobody, ip()).andExpect(redirectedUrl("/forgotPassword?sent")).andReturn();
    assertThat(known.getResponse().getContentAsString()).isEqualTo(unknown.getResponse().getContentAsString());

    ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
    verify(mailer, timeout(5000)).send(eq(email), eq("Crypto Wallet - reset your password"), text.capture());
    assertThat(text.getValue()).contains("30 minutes", "Nothing in your wallet is deleted");
    verify(mailer, after(300).never()).send(eq(nobody), anyString(), anyString());
  }

  @Test
  void theLinkSetsANewPasswordOnceAndTheWalletStays() throws Exception {
    String email = account();
    forgot(email.toUpperCase(), ip());
    String token = mailedToken(email);

    List<String> stored = jdbc.queryForList("select token_hash from password_reset_tokens", String.class);
    assertThat(stored).contains(PasswordResetService.hash(token)).doesNotContain(token);

    // a refused password does not use up the link
    mvc.perform(post("/resetPassword").with(csrf())
            .param("token", token).param("password", "new-password").param("confirm", "other-password"))
        .andExpect(model().attributeExists("error"));
    reset(token, "short").andExpect(model().attributeExists("error"));

    reset(token, "new-password").andExpect(redirectedUrl("/login?reset"));
    assertThat(users.passwordMatches(email, "new-password")).isTrue();
    assertThat(users.passwordMatches(email, "old-password")).isFalse();
    // nothing deleted: the keys encrypted with the old password are still there
    assertThat(jdbc.queryForObject("select wallet from users where email = ?", String.class, email)).isEqualTo(WALLET);

    // single use
    reset(token, "third-password").andExpect(model().attribute("badLink", true));
    reset("not-a-token", "third-password").andExpect(model().attribute("badLink", true));
    assertThat(users.passwordMatches(email, "new-password")).isTrue();
  }

  @Test
  void anExpiredLinkIsRefused() throws Exception {
    String email = account();
    forgot(email, ip());
    String token = mailedToken(email);
    jdbc.update("update password_reset_tokens set expires_at = ? where token_hash = ?",
        java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(60)), PasswordResetService.hash(token));
    reset(token, "new-password").andExpect(model().attribute("badLink", true));
    assertThat(users.passwordMatches(email, "old-password")).isTrue();
  }

  @Test
  void aNewPasswordCancelsTheOtherLinks() throws Exception {
    String email = account();
    String ip = ip();
    forgot(email, ip);
    forgot(email, ip);
    ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
    verify(mailer, timeout(5000).times(2)).send(eq(email), anyString(), text.capture());
    List<String> tokens = text.getAllValues().stream().map(t -> {
      Matcher m = LINK.matcher(t);
      assertThat(m.find()).isTrue();
      return m.group(1);
    }).toList();
    assertThat(tokens.get(0)).isNotEqualTo(tokens.get(1));
    reset(tokens.get(1), "new-password").andExpect(redirectedUrl("/login?reset"));
    reset(tokens.get(0), "other-password").andExpect(model().attribute("badLink", true));
  }

  @Test
  void atMostThreeMailsPerEmailWithTheSameAnswer() throws Exception {
    String email = account();
    for (int i = 0; i < 5; i++) {
      forgot(email, ip()).andExpect(redirectedUrl("/forgotPassword?sent"));
    }
    verify(mailer, after(500).times(3)).send(eq(email), anyString(), anyString());
  }

  @Test
  void tooManyRequestsFromOneAddress() throws Exception {
    String ip = ip();
    for (int i = 0; i < 10; i++) {
      forgot(email(), ip).andExpect(redirectedUrl("/forgotPassword?sent"));
    }
    forgot(email(), ip).andExpect(status().isTooManyRequests());
    forgot(email(), ip()).andExpect(redirectedUrl("/forgotPassword?sent"));
  }

  @Test
  void theFormNeedsTheCsrfToken() throws Exception {
    mvc.perform(post("/forgotPassword").param("email", email())).andExpect(status().isForbidden());
  }
}
