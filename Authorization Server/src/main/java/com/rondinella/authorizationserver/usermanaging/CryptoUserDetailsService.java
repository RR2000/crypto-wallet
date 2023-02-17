package com.rondinella.authorizationserver.usermanaging;

import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class CryptoUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public CryptoUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    CryptoUserDatabase cryptoUserDatabase = userRepository.findByEmail(username);
    if (cryptoUserDatabase == null) {
      throw new UsernameNotFoundException(username);
    }
    return new CryptoUserPrincipal(cryptoUserDatabase);
  }

  public OidcUserInfo loadOidcUserByUsername(String username, Set<String> scopes) {
    CryptoUserDatabase cryptoUserDatabase = userRepository.findByEmail(username);
    if (cryptoUserDatabase == null) {
      throw new UsernameNotFoundException(username);
    }
    ;
    return new CryptoUserOidc(cryptoUserDatabase, scopes);
  }

  @Transactional
  public void addCryptoUser(CryptoUserDatabase user) {
    CryptoUserDatabase userName = new CryptoUserDatabase();
    userName.setEmail(user.getEmail());
    userName.setPassword(user.getPassword());
    userRepository.save(userName);
    userRepository.save(user);
  }

  @Transactional
  public void updateCryptoUserWallet(String username, String wallet) {
    CryptoUserDatabase user = userRepository.findByEmail(username);
    user.setWallet(wallet);
    userRepository.save(user);
  }

  @Transactional
  public void updateCryptoUserPassword(String username, String newEncodedPassword) {
    CryptoUserDatabase user = userRepository.findByEmail(username);
    user.setPassword(newEncodedPassword);
    userRepository.save(user);
  }
}
