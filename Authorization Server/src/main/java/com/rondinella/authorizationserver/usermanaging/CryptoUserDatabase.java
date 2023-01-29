package com.rondinella.authorizationserver.usermanaging;

import jakarta.persistence.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.repository.cdi.Eager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class CryptoUserDatabase {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @Column(nullable = false)
  private String username;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private boolean isAccountExpired;

  @Column(nullable = false)
  private boolean isAccountLocked;

  @Column(nullable = false)
  private boolean isCredentialsExpired;

  @Column(nullable = false)
  private boolean isEnabled;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "authorities")
  private List<String> stringAuthorities = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Collection<? extends GrantedAuthority> getAuthorities() {
    List<GrantedAuthority> authorities = new ArrayList<>();

    for (String role : stringAuthorities) {
      Assert.isTrue(!role.startsWith("ROLE_"), () -> {
        return role + " cannot start with ROLE_ (it is automatically added)";
      });
      authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
    }

    return authorities;
  }

  public void addAuthority(String authority) {
    if(this.stringAuthorities == null)
      this.stringAuthorities = new ArrayList<>();

    this.stringAuthorities.add(authority);
  }

  public String getPassword() {
    return this.password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getUsername() {
    return this.username;
  }

  public void setUsername(String username){
    this.username = username;
  }

  public boolean isAccountNonExpired() {
    return !this.isAccountExpired;
  }

  public boolean isAccountNonLocked() {
    return !this.isAccountLocked;
  }

  public boolean isCredentialsNonExpired() {
    return !isCredentialsExpired;
  }

  public boolean isEnabled() {
    return this.isEnabled;
  }

}
