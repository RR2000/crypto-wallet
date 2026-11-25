package com.rondinella.authorizationserver.web;

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
    if (newPassword.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    return users.changePassword(jwt.getSubject(), currentPassword, newPassword)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.status(403).build();
  }
}
