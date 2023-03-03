package com.rondinella.authorizationserver.config;

import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.UUID;
import java.util.function.Function;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class AuthorizationServerConfig {

  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder().build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("*");
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    return source;
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
                                                                    CryptoUserDetailsService service,
                                                                    CorsConfigurationSource corsConfigurationSource,
                                                                    RegisteredClientRepository registeredClientRepository) throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
        new OAuth2AuthorizationServerConfigurer();
    RequestMatcher endpointsMatcher = authorizationServerConfigurer
        .getEndpointsMatcher();

    Function<OidcUserInfoAuthenticationContext, OidcUserInfo> userInfoMapper = (context) -> {
      OidcUserInfoAuthenticationToken authentication = context.getAuthentication();
      JwtAuthenticationToken principal = (JwtAuthenticationToken) authentication.getPrincipal();

      return service.loadOidcUserByUsername(principal.getName(), context.getAccessToken().getScopes());
    };

    authorizationServerConfigurer
        .tokenRevocationEndpoint(withDefaults())
        .oidc((oidc) -> oidc
            .userInfoEndpoint((userInfo) -> userInfo
                .userInfoMapper(userInfoMapper)
            )
        );
    http.headers().frameOptions().disable().and()
        .securityMatcher(endpointsMatcher)
        .authorizeHttpRequests((authorize) -> authorize
            .anyRequest().authenticated()
        )
        .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
        .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
        .exceptionHandling((exceptions) -> exceptions
            .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
        )
        .apply(authorizationServerConfigurer);

    http.cors().configurationSource(corsConfigurationSource);

    return http.build();
  }


  @Bean
  public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource)
      throws Exception {

    http.headers().frameOptions().disable().and()
        .authorizeHttpRequests((authorize) ->
            authorize
                .requestMatchers("/newUser").permitAll()
                .requestMatchers("/addUser").permitAll()
                .anyRequest().authenticated()
        )
        .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)

        // Form login handles the redirect to the login page from the
        // authorization server filter chain
        //.formLogin()
        .logout(withDefaults())
        .formLogin(withDefaults());
    //.loginPage("/login")
    //.permitAll();

    http.cors().configurationSource(corsConfigurationSource);

    return http.build();
  }

  @Bean
  public RegisteredClientRepository registeredClientRepository(CryptoUserDetailsService service, PasswordEncoder passwordEncoder) {

    RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId("crypto-client")
        .clientSecret(passwordEncoder.encode("secret"))
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .redirectUri("http://127.0.0.1:8080/login/oauth2/code/messaging-client-oidc")
        .redirectUri("http://127.0.0.1:5173/callback")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .scope("wallets.private_keys")
        .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
        .build();

    return new InMemoryRegisteredClientRepository(registeredClient);
  }
}
