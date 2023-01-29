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

  @Column
  private String firstName;

  @Column
  private String lastName;

  @Column
  private String gender;

  @Column
  private String birthdate;

  @Column
  private String locale;

  @Column
  private String email;

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
  @Column(nullable = false)
  private List<String> stringAuthorities = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "wallets")
  private List<String> wallets = new ArrayList<>();

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

  public void setAuthorities(List<String> stringAuthorities) {
    this.stringAuthorities = stringAuthorities;
  }

  public void addWallet(String wallet){
    this.wallets.add(wallet);
  }

  public void setWallets(List<String> wallets) {
    this.wallets = wallets;
  }

  public List<String> getWallets() {
    return wallets;
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

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public String getBirthdate() {
    return birthdate;
  }

  public void setBirthdate(String birthdate) {
    this.birthdate = birthdate;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public static class CryptoUserDatabaseBuilder {
    private Long id;
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String gender;
    private String birthdate;
    private String locale;
    private String email;
    private boolean isAccountExpired;
    private boolean isAccountLocked;
    private boolean isCredentialsExpired;
    private boolean isEnabled;
    private List<String> stringAuthorities;
    private List<String> wallets;

    public CryptoUserDatabaseBuilder setId(Long id) {
      this.id = id;
      return this;
    }

    public CryptoUserDatabaseBuilder setUsername(String username) {
      this.username = username;
      return this;
    }

    public CryptoUserDatabaseBuilder setPassword(String password) {
      this.password = password;
      return this;
    }

    public CryptoUserDatabaseBuilder setFirstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    public CryptoUserDatabaseBuilder setLastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    public CryptoUserDatabaseBuilder setGender(String gender) {
      this.gender = gender;
      return this;
    }

    public CryptoUserDatabaseBuilder setBirthdate(String birthdate) {
      this.birthdate = birthdate;
      return this;
    }

    public CryptoUserDatabaseBuilder setLocale(String locale) {
      this.locale = locale;
      return this;
    }

    public CryptoUserDatabaseBuilder setEmail(String email) {
      this.email = email;
      return this;
    }

    public CryptoUserDatabaseBuilder setIsAccountExpired(boolean isAccountExpired) {
      this.isAccountExpired = isAccountExpired;
      return this;
    }

    public CryptoUserDatabaseBuilder setIsAccountLocked(boolean isAccountLocked) {
      this.isAccountLocked = isAccountLocked;
      return this;
    }

    public CryptoUserDatabaseBuilder setIsCredentialsExpired(boolean isCredentialsExpired) {
      this.isCredentialsExpired = isCredentialsExpired;
      return this;
    }

    public CryptoUserDatabaseBuilder setIsEnabled(boolean isEnabled) {
      this.isEnabled = isEnabled;
      return this;
    }

    public CryptoUserDatabaseBuilder setStringAuthorities(List<String> stringAuthorities) {
      this.stringAuthorities = stringAuthorities;
      return this;
    }

    public CryptoUserDatabaseBuilder setWallets(List<String> wallets) {
      this.wallets = wallets;
      return this;
    }

    public CryptoUserDatabase build() {
      CryptoUserDatabase cryptoUserDatabase = new CryptoUserDatabase();
      cryptoUserDatabase.setId(id);
      cryptoUserDatabase.setUsername(username);
      cryptoUserDatabase.setPassword(password);
      cryptoUserDatabase.setFirstName(firstName);
      cryptoUserDatabase.setLastName(lastName);
      cryptoUserDatabase.setGender(gender);
      cryptoUserDatabase.setBirthdate(birthdate);
      cryptoUserDatabase.setLocale(locale);
      cryptoUserDatabase.setEmail(email);
      cryptoUserDatabase.setIsEnabled(isEnabled);
      cryptoUserDatabase.setIsAccountExpired(isAccountExpired);
      cryptoUserDatabase.setIsAccountLocked(isAccountLocked);
      cryptoUserDatabase.setIsCredentialsExpired(isCredentialsExpired);
      cryptoUserDatabase.setAuthorities(stringAuthorities);
      cryptoUserDatabase.setWallets(wallets);

      return cryptoUserDatabase;
    }
  }
}
