package com.rondinella.authorizationserver.usermanaging;

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
    if (email.isEmpty() || password.length() < 8) {
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
    return passwordEncoder.matches(password, find(username).getPassword());
  }

  @Transactional
  public boolean changePassword(String username, String currentPassword, String newPassword) {
    CryptoUserDatabase user = find(username);
    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      return false;
    }
    user.setPassword(passwordEncoder.encode(newPassword));
    return true;
  }

  private CryptoUserDatabase find(String username) {
    CryptoUserDatabase user = userRepository.findByEmail(username);
    if (user == null) {
      throw new UsernameNotFoundException(username);
    }
    return user;
  }
}
