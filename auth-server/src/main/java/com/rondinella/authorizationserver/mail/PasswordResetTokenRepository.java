package com.rondinella.authorizationserver.mail;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

  /** Uses the token if it is still good: 1 the first time, 0 if used, expired or unknown (also when two race). */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update PasswordResetToken t set t.usedAt = :now where t.tokenHash = :hash and t.usedAt is null"
      + " and t.expiresAt > :now")
  int use(@Param("hash") String hash, @Param("now") Instant now);

  /** After a new password the other links of the account stop working. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update PasswordResetToken t set t.usedAt = :now where t.userEmail = :email and t.usedAt is null")
  int useAll(@Param("email") String email, @Param("now") Instant now);

  @Modifying
  @Query("delete from PasswordResetToken t where t.expiresAt < :before")
  int deleteExpiredBefore(@Param("before") Instant before);
}
