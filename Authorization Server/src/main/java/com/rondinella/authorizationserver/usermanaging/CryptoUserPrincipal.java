package com.rondinella.authorizationserver.usermanaging;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
public class CryptoUserPrincipal implements UserDetails {
  private final CryptoUserDatabase cryptoUserDatabase;

  public CryptoUserPrincipal(CryptoUserDatabase cryptoUserDatabase) {
    this.cryptoUserDatabase = cryptoUserDatabase;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return cryptoUserDatabase.getAuthorities();
  }

  @Override
  public String getPassword() {
    return cryptoUserDatabase.getPassword();
  }

  @Override
  public String getUsername() {
    return cryptoUserDatabase.getUsername();
  }

  @Override
  public boolean isAccountNonExpired() {
    return cryptoUserDatabase.isAccountNonExpired();
  }

  @Override
  public boolean isAccountNonLocked() {
    return cryptoUserDatabase.isAccountNonLocked();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return cryptoUserDatabase.isCredentialsNonExpired();
  }

  @Override
  public boolean isEnabled() {
    return cryptoUserDatabase.isEnabled();
  }
}
