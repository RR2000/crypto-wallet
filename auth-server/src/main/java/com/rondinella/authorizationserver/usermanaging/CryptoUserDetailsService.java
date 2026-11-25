package com.rondinella.authorizationserver.usermanaging;

import java.util.List;
import java.util.Set;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CryptoUserDetailsService implements UserDetailsService {

  public static final int MIN_PASSWORD_LENGTH = 8;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public CryptoUserDetailsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    return new CryptoUserPrincipal(find(username));
  }

  public OidcUserInfo loadOidcUserByUsername(String username, Set<String> scopes) {
    return new CryptoUserOidc(find(username), scopes);
  }

  /** Registers a new user; returns an error message, or null if the user was created. */
  @Transactional
  public String register(CryptoUserDatabase form) {
    String email = form.getEmail() == null ? "" : form.getEmail().trim().toLowerCase();
    String password = form.getPassword() == null ? "" : form.getPassword();
    if (email.isEmpty() || !validPassword(password)) {
      return "Email and a password of at least 8 characters are required.";
    }
    // never overwrite an existing account
    if (userRepository.existsById(email)) {
      return "An account with this email already exists.";
    }
    CryptoUserDatabase user = new CryptoUserDatabase();
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(password));
    user.setFirstName(form.getFirstName());
    user.setLastName(form.getLastName());
    user.addAuthority("USER");
    userRepository.save(user);
    return null;
  }

  @Transactional
  public void updateCryptoUserWallet(String username, String wallet) {
    find(username).setWallet(wallet);
  }

  @Transactional(readOnly = true)
  public boolean passwordMatches(String username, String password) {
    String hash = find(username).getPassword();
    return hash != null && passwordEncoder.matches(password, hash);
  }

  public static boolean validPassword(String password) {
    return password != null && password.length() >= MIN_PASSWORD_LENGTH;
  }

  @Transactional
  public boolean changePassword(String username, String currentPassword, String newPassword) {
    if (!passwordMatches(username, currentPassword)) {
      return false;
    }
    find(username).setPassword(passwordEncoder.encode(newPassword));
    return true;
  }

  /** First password of an account created from GitHub; refused if the account already has one. */
  @Transactional
  public boolean setFirstPassword(String username, String newPassword) {
    CryptoUserDatabase user = find(username);
    if (user.getPassword() != null) {
      return false;
    }
    user.setPassword(passwordEncoder.encode(newPassword));
    return true;
  }

  /**
   * New password without the old one, for whoever proved to own the email through GitHub. The wallet is not
   * touched: the keys encrypted with the old password stay there, locked, until the old password turns up again.
   */
  @Transactional
  public void resetPassword(String username, String newPassword) {
    find(username).setPassword(passwordEncoder.encode(newPassword));
  }

  /**
   * The local account of a GitHub user: the existing account with one of their verified emails, otherwise a new
   * account (without password) on the first of them. {@code verifiedEmails} has the primary email first.
   */
  @Transactional
  public CryptoUserDatabase linkOrCreate(List<String> verifiedEmails, String firstName, String lastName) {
    List<String> emails = verifiedEmails.stream().map(e -> e.trim().toLowerCase()).filter(e -> !e.isEmpty()).toList();
    if (emails.isEmpty()) {
      throw new IllegalArgumentException("no verified email");
    }
    for (String email : emails) {
      CryptoUserDatabase existing = userRepository.findByEmail(email);
      if (existing != null) {
        return existing;
      }
    }
    CryptoUserDatabase user = new CryptoUserDatabase();
    user.setEmail(emails.getFirst());
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.addAuthority("USER");
    return userRepository.save(user);
  }

  private CryptoUserDatabase find(String username) {
    CryptoUserDatabase user = userRepository.findByEmail(username);
    if (user == null) {
      throw new UsernameNotFoundException(username);
    }
    return user;
  }
}
