package com.rondinella.authorizationserver.mail;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A "forgot password" link. Only the SHA-256 of the random token is stored: whoever reads the database cannot use it.
 * Single use ({@code usedAt}) and short lived ({@code expiresAt}).
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

  @Id
  @Column(name = "token_hash", length = 64)
  private String tokenHash;

  @Column(name = "user_email", nullable = false, length = 320)
  private String userEmail;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  protected PasswordResetToken() {
  }

  public PasswordResetToken(String tokenHash, String userEmail, Instant createdAt, Instant expiresAt) {
    this.tokenHash = tokenHash;
    this.userEmail = userEmail;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }
}
