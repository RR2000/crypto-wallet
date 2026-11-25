package com.rondinella.authorizationserver.github;

import com.rondinella.authorizationserver.usermanaging.CryptoUserDatabase;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Sign in with GitHub: the GitHub account is mapped to the local account with the same verified email (or a new one
 * without password). The principal's name is the local email, like after a password login, so tokens and the
 * profile do not depend on how the user signed in.
 */
@Service
public class GitHubUserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  /** One entry of https://api.github.com/user/emails (scope user:email). */
  public record GitHubEmail(String email, boolean primary, boolean verified) {
  }

  private final CryptoUserDetailsService users;
  private final OAuth2UserService<OAuth2UserRequest, OAuth2User> gitHubProfile = new DefaultOAuth2UserService();
  private final RestClient api = RestClient.create("https://api.github.com");

  GitHubUserService(CryptoUserDetailsService users) {
    this.users = users;
  }

  @Override
  public OAuth2User loadUser(OAuth2UserRequest request) {
    OAuth2User profile = gitHubProfile.loadUser(request);
    List<GitHubEmail> emails = api.get().uri("/user/emails")
        .header("Authorization", "Bearer " + request.getAccessToken().getTokenValue())
        .header("Accept", "application/vnd.github+json")
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
    return toLocalUser(emails == null ? List.of() : emails, profile.getAttributes());
  }

  /** Separate from the HTTP calls, so it can be tested without GitHub. */
  public OAuth2User toLocalUser(List<GitHubEmail> emails, Map<String, Object> gitHubAttributes) {
    // only verified emails prove ownership: an unverified one could be anybody's
    List<String> verified = emails.stream()
        .filter(GitHubEmail::verified)
        .sorted(Comparator.comparing(GitHubEmail::primary).reversed())
        .map(GitHubEmail::email)
        .toList();
    if (verified.isEmpty()) {
      throw new OAuth2AuthenticationException(new OAuth2Error("no_verified_email",
          "The GitHub account has no verified email.", null));
    }
    String name = Objects.toString(gitHubAttributes.get("name"), "").trim();
    String login = Objects.toString(gitHubAttributes.get("login"), "");
    String first = name.isEmpty() ? login : name.split("\\s+", 2)[0];
    String last = name.contains(" ") ? name.split("\\s+", 2)[1] : "";

    CryptoUserDatabase user = users.linkOrCreate(verified, first, last);
    if (!user.isEnabled() || !user.isAccountNonLocked()) {
      throw new OAuth2AuthenticationException(new OAuth2Error("account_disabled", "Account disabled.", null));
    }
    return new DefaultOAuth2User(user.getAuthorities(), Map.of("email", user.getEmail(), "login", login), "email");
  }
}
