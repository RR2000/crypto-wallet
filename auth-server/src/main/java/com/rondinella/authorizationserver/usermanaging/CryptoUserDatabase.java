package com.rondinella.authorizationserver.usermanaging;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.Assert;

import java.util.*;

@Entity
@Table(name = "users")
public class CryptoUserDatabase {

  @Id
  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column
  private String firstName;

  @Column
  private String lastName;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "authorities", joinColumns = @JoinColumn(name = "user_email"))
  @Column(name = "authority")
  private List<String> grantedAuthorities = new ArrayList<>();

  @Column(columnDefinition = "text")
  private String wallet = "[[],[],[]]";

  @Column(nullable = false)
  private boolean isAccountExpired = false;

  @Column(nullable = false)
  private boolean isAccountLocked = false;

  @Column(nullable = false)
  private boolean isCredentialsExpired = false;

  @Column(nullable = false)
  private boolean isEnabled = true;

  public Collection<? extends GrantedAuthority> getAuthorities() {
    List<GrantedAuthority> authorities = new ArrayList<>();

    for (String role : grantedAuthorities) {
      Assert.isTrue(!role.startsWith("ROLE_"), () -> {
        return role + " cannot start with ROLE_ (it is automatically added)";
      });
      authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
    }

    return authorities;
  }

  public void addAuthority(String stringAuthority){
    ArrayList<String> auth = new ArrayList<>();
    auth.add(stringAuthority);
    addAuthorities(auth);
  }

  public void addAuthorities(List<String> stringAuthorities) {
    if(this.grantedAuthorities == null)
      this.grantedAuthorities = new ArrayList<>();

    this.grantedAuthorities.addAll(stringAuthorities);
  }

  public String getWallet() {
    return wallet;
  }

  public void setWallet(String wallet) {
    this.wallet = wallet;
  }

  public String getPassword() {
    return this.password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isAccountNonExpired() {
    return !this.isAccountExpired;
  }

  private void setIsAccountLocked(boolean isAccountLocked) {
    this.isAccountLocked = isAccountLocked;
  }

  public boolean isAccountNonLocked() {
    return !this.isAccountLocked;
  }

  private void setIsCredentialsExpired(boolean isCredentialsExpired) {
    this.isCredentialsExpired = isCredentialsExpired;
  }

  public boolean isCredentialsNonExpired() {
    return !isCredentialsExpired;
  }

  private void setIsAccountExpired(boolean isAccountExpired) {
    this.isAccountExpired = isAccountExpired;
  }

  public boolean isEnabled() {
    return this.isEnabled;
  }

  private void setIsEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
