package com.rondinella.authorizationserver.usermanaging;

import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CryptoUserOidc extends OidcUserInfo {


  public CryptoUserOidc(CryptoUserDatabase cryptoUserDatabase, Set<String> scopes){
    super(findByUsername(cryptoUserDatabase, scopes));
  }

  public static Map<String, Object> findByUsername(CryptoUserDatabase db, Set<String> scopes) {
    String subject = db.getUsername();
    OidcUserInfo.Builder builder = OidcUserInfo.builder().subject(subject);
    if (!scopes.isEmpty()) {
      if (scopes.contains(OidcScopes.PROFILE)) {
        builder.name(db.getFirstName() + " " + db.getLastName())
            .givenName(db.getFirstName())
            .familyName(db.getLastName())
            .nickname(subject)
            .preferredUsername(subject)
            .gender(db.getGender())
            .birthdate(db.getBirthdate())
            .locale(db.getLocale())
            .updatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
      }
      if (scopes.contains(OidcScopes.EMAIL)) {
        builder.email(db.getEmail()).emailVerified(false);
      }
      if (scopes.contains("wallets.private_keys")) {
        builder.claim("private_keys", db.getWallets());
      }
    }

    return builder.build().getClaims();
  }
}
