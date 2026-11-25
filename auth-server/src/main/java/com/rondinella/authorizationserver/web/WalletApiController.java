package com.rondinella.authorizationserver.web;

import com.rondinella.authorizationserver.config.AuthorizationServerConfig;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API used by the web app with the user's access token. The wallet arrives already encrypted in the browser
 * with the account password: the server only ever stores ciphertext.
 */
@RestController
@RequestMapping("/api")
public class WalletApiController {

  private static final int MAX_WALLET_LENGTH = 1_000_000;

  private final CryptoUserDetailsService users;

  WalletApiController(CryptoUserDetailsService users) {
    this.users = users;
  }

  @PostMapping("/wallet")
  public ResponseEntity<Void> updateWallet(@AuthenticationPrincipal Jwt jwt, @RequestParam String wallet) {
    if (wallet.length() > MAX_WALLET_LENGTH) {
      return ResponseEntity.badRequest().build();
    }
    users.updateCryptoUserWallet(jwt.getSubject(), wallet);
    return ResponseEntity.noContent().build();
  }

  /** The web app checks the password here before using it to decrypt or re-encrypt the wallet. */
  @PostMapping("/password/verify")
  public ResponseEntity<Void> verifyPassword(@AuthenticationPrincipal Jwt jwt, @RequestParam String password) {
    return users.passwordMatches(jwt.getSubject(), password)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.status(403).build();
  }

  @PostMapping("/password")
  public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam String currentPassword,
                                             @RequestParam String newPassword) {
    if (!CryptoUserDetailsService.validPassword(newPassword)) {
      return ResponseEntity.badRequest().build();
    }
    return users.changePassword(jwt.getSubject(), currentPassword, newPassword)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.status(403).build();
  }

  /** First password of an account that came from GitHub: 409 if it already has one. */
  @PostMapping("/password/set")
  public ResponseEntity<Void> setPassword(@AuthenticationPrincipal Jwt jwt, @RequestParam String newPassword) {
    if (!CryptoUserDetailsService.validPassword(newPassword)) {
      return ResponseEntity.badRequest().build();
    }
    return users.setFirstPassword(jwt.getSubject(), newPassword)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.status(409).build();
  }

  /**
   * Forgotten password: a new one without the old one, only with a token obtained by signing in with GitHub.
   * The wallet stays as it is; the keys encrypted with the old password are recovered in the browser if it turns up.
   */
  @PostMapping("/password/reset")
  public ResponseEntity<Void> resetPassword(@AuthenticationPrincipal Jwt jwt, @RequestParam String newPassword) {
    if (!AuthorizationServerConfig.LOGIN_GITHUB.equals(jwt.getClaimAsString(AuthorizationServerConfig.LOGIN_CLAIM))) {
      return ResponseEntity.status(403).build();
    }
    if (!CryptoUserDetailsService.validPassword(newPassword)) {
      return ResponseEntity.badRequest().build();
    }
    users.resetPassword(jwt.getSubject(), newPassword);
    return ResponseEntity.noContent().build();
  }
}
