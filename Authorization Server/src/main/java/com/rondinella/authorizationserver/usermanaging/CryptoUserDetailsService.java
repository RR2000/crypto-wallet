package com.rondinella.authorizationserver.usermanaging;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    CryptoUserDatabase cryptoUserDatabase = userRepository.findByUsername(username);
    if (cryptoUserDatabase == null) {
      throw new UsernameNotFoundException(username);
    }
    return new CryptoUserPrincipal(cryptoUserDatabase);
  }

  public OidcUserInfo loadOidcUserByUsername(String username, Set<String> scopes){
    CryptoUserDatabase cryptoUserDatabase = userRepository.findByUsername(username);
    if (cryptoUserDatabase == null) {
      throw new UsernameNotFoundException(username);
    };
    return new CryptoUserOidc(cryptoUserDatabase, scopes);
  }

  public void addCryptoUser(CryptoUserDatabase user){
    userRepository.save(user);
  }


}
