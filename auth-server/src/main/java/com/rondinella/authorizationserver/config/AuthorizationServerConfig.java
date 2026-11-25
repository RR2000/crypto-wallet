package com.rondinella.authorizationserver.config;

import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Three filter chains:
 * <ol>
 *   <li>the OAuth2 / OpenID Connect endpoints (authorize, token, userinfo, jwks, logout...);</li>
 *   <li>the JSON API used by the wallet web app, authenticated with the access token;</li>
 *   <li>the pages of the server itself: login and registration.</li>
 * </ol>
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class AuthorizationServerConfig {

  public static final String WALLET_SCOPE = "wallet";

  @Bean
  @Order(1)
  SecurityFilterChain authorizationServerChain(HttpSecurity http, CryptoUserDetailsService users) throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServer = new OAuth2AuthorizationServerConfigurer();
    http
        .securityMatcher(authorizationServer.getEndpointsMatcher())
        .with(authorizationServer, server -> server
            .oidc(oidc -> oidc
                .userInfoEndpoint(userInfo -> userInfo
                    .userInfoMapper(context -> {
                      JwtAuthenticationToken principal = (JwtAuthenticationToken) context.getAuthentication().getPrincipal();
                      return users.loadOidcUserByUsername(principal.getName(), context.getAccessToken().getScopes());
                    }))))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
        // the browser goes to the login page, an API client gets a 401
        .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
            new LoginUrlAuthenticationEntryPoint("/login"), new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/api/**")
        .authorizeHttpRequests(authorize -> authorize.anyRequest().hasAuthority("SCOPE_" + WALLET_SCOPE))
        .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // bearer token only, no cookies: CSRF does not apply
        .csrf(csrf -> csrf.disable());
    return http.build();
  }

  @Bean
  @Order(3)
  SecurityFilterChain pagesChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/login", "/newUser", "/addUser", "/auth.css", "/error", "/actuator/health/**").permitAll()
            .anyRequest().authenticated())
        .formLogin(form -> form.loginPage("/login").permitAll())
        .logout(Customizer.withDefaults());
    return http.build();
  }

  /** The wallet web app: a public client (it runs in the browser, it cannot keep a secret), so PKCE is required. */
  @Bean
  RegisteredClientRepository registeredClientRepository(@Value("${wallet.web-url}") String webUrl) {
    RegisteredClient web = RegisteredClient.withId("wallet-web")
        .clientId("wallet-web")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri(webUrl + "/callback")
        .postLogoutRedirectUri(webUrl + "/")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .scope(WALLET_SCOPE)
        .clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(true).build())
        .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofHours(1)).build())
        .build();
    return new InMemoryRegisteredClientRepository(web);
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
