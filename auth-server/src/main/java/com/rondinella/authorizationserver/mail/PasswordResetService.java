package com.rondinella.authorizationserver.mail;

import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import com.rondinella.authorizationserver.usermanaging.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Forgot password" by email, the standard flow:
 * <ul>
 *   <li>the request always gets the same answer, whether an account has that email or not, and the mail leaves in the
 *   background (same timing);</li>
 *   <li>the link holds 32 random bytes; the database keeps only their SHA-256, with an expiry of 30 minutes and the
 *   moment it was used: it works once, and a new password makes the other links of the account useless;</li>
 *   <li>limits per client address, per email and for the whole site ({@link MailLimits});</li>
 *   <li>the link points to the web app ({@code wallet.web-url}), never to the Host of the request, and carries the
 *   token in the fragment: it never reaches an access log.</li>
 * </ul>
 * Like the reset after a GitHub login, the wallet is not touched: the keys encrypted in the browser with the old
 * password stay there, locked, and open again with it. Emails and tokens never go to the logs.
 */
@Service
public class PasswordResetService {

  public static final Duration TTL = Duration.ofMinutes(30);
  private static final Pattern EMAIL = Pattern.compile("[^@\\s]{1,64}@[^@\\s]+\\.[^@\\s]+");

  private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
  private static final SecureRandom RANDOM = new SecureRandom();

  public enum Request { ACCEPTED, INVALID_EMAIL, TOO_MANY }

  public enum Reset { DONE, BAD_LINK, BAD_PASSWORD }

  private final MailSettings settings;
  private final Mailer mailer;
  private final ExecutorService mailExecutor;
  private final MailLimits limits;
  private final PasswordResetTokenRepository tokens;
  private final UserRepository users;
  private final CryptoUserDetailsService userService;
  private final Clock clock;

  @Autowired
  public PasswordResetService(MailSettings settings, Mailer mailer,
                              @Qualifier("mailExecutor") ExecutorService mailExecutor, MailLimits limits,
                              PasswordResetTokenRepository tokens, UserRepository users,
                              CryptoUserDetailsService userService) {
    this(settings, mailer, mailExecutor, limits, tokens, users, userService, Clock.systemUTC());
  }

  PasswordResetService(MailSettings settings, Mailer mailer, ExecutorService mailExecutor, MailLimits limits,
                       PasswordResetTokenRepository tokens, UserRepository users,
                       CryptoUserDetailsService userService, Clock clock) {
    this.settings = settings;
    this.mailer = mailer;
    this.mailExecutor = mailExecutor;
    this.limits = limits;
    this.tokens = tokens;
    this.users = users;
    this.userService = userService;
    this.clock = clock;
  }

  public boolean enabled() {
    return settings.enabled();
  }

  /** Sends a link to the account with this email, if there is one. ACCEPTED either way. */
  @Transactional
  public Request requestReset(String rawEmail, String clientIp) {
    String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
    if (email.length() > 254 || !EMAIL.matcher(email).matches()) {
      return Request.INVALID_EMAIL;
    }
    if (!limits.allowIp(clientIp)) {
      return Request.TOO_MANY;
    }
    if (users.findByEmail(email) == null || !limits.allowMail(email)) {
      return Request.ACCEPTED;
    }
    Instant now = clock.instant();
    tokens.deleteExpiredBefore(now.minus(Duration.ofDays(1)));
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    tokens.save(new PasswordResetToken(hash(token), email, now, now.plus(TTL)));
    String link = settings.web() + "/auth/resetPassword#token=" + token;
    send(email, "Crypto Wallet - reset your password", """
        Hi,

        someone asked to reset the password of the Crypto Wallet account of this email address.
        To choose a new one, open this link within 30 minutes:

        %1$s

        The link works only once. Nothing in your wallet is deleted: the accounts encrypted with the old password
        stay there, locked, and open again with the old password if you remember it.

        If it wasn't you, ignore this email: your password stays the same.

        --
        Crypto Wallet, %2$s
        Automatic message, replies are not read.
        """.formatted(link, settings.web()));
    return Request.ACCEPTED;
  }

  /** New password with a link from {@link #requestReset}; the wallet stays as it is. */
  @Transactional
  public Reset resetPassword(String token, String newPassword) {
    if (!CryptoUserDetailsService.validPassword(newPassword)
        || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
      return Reset.BAD_PASSWORD;
    }
    if (token == null || token.isBlank() || token.length() > 100) {
      return Reset.BAD_LINK;
    }
    String hash = hash(token);
    Instant now = clock.instant();
    if (tokens.use(hash, now) != 1) {
      return Reset.BAD_LINK;
    }
    PasswordResetToken used = tokens.findById(hash).orElse(null);
    if (used == null || users.findByEmail(used.getUserEmail()) == null) {
      return Reset.BAD_LINK;
    }
    userService.resetPassword(used.getUserEmail(), newPassword);
    tokens.useAll(used.getUserEmail(), now);
    log.info("Password reset by email for one account");
    return Reset.DONE;
  }

  public static String hash(String token) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private void send(String to, String subject, String text) {
    mailExecutor.execute(() -> {
      try {
        mailer.send(to, subject, text);
      } catch (RuntimeException e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
          root = root.getCause();
        }
        log.warn("Mail not sent: {}", root.getClass().getName());
      }
    });
  }
}
