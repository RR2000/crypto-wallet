package com.rondinella.authorizationserver.service;

import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Set;

@Service
public class OidcUserInfoService {

  private final static UserInfoRepository userInfoRepository = new UserInfoRepository();

  public static OidcUserInfo loadUser(String username, Set<String> scopes) {
    return userInfoRepository.findByUsername(username, scopes);
  }

  static class UserInfoRepository {
    public OidcUserInfo findByUsername(String name, Set<String> scopes) {
      OidcUserInfo.Builder builder = OidcUserInfo.builder().subject(name);
      if (!scopes.isEmpty()) {
        if (scopes.contains(OidcScopes.PROFILE)) {
          builder.name("First Last")
              .givenName("First")
              .familyName("Last")
              .middleName("Middle")
              .nickname("User")
              .preferredUsername(name)
              .profile("http://127.0.0.1:8080/" + name)
              .picture("http://127.0.0.1:8080/" + name + ".jpg")
              .website("http://127.0.0.1:8080/")
              .gender("female")
              .birthdate("2022-05-24")
              .zoneinfo("China/Beijing")
              .locale("zh-cn")
              .updatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (scopes.contains(OidcScopes.EMAIL)) {
          builder.email(name + "@163.com").emailVerified(false);
        }
        if (scopes.contains(OidcScopes.ADDRESS)) {
          builder.address("Champ de Mars\n5 Av. Anatole France\n75007 Paris\nFrance");
        }
        if (scopes.contains(OidcScopes.PHONE)) {
          builder.phoneNumber("13728903134").phoneNumberVerified(false);
        }
        if (scopes.contains("wallets.private_keys")){
          ArrayList<String> privateKeys = new ArrayList<>();
          privateKeys.add("bshjdlvba4uytbvy7aw48vtawytov74woa4vtba");
          privateKeys.add("dasdfsgbye5yw5ey54y4w5by");
          privateKeys.add("tvw3twe4vt54uwv64j65wuw5v5uv56v");
          builder.claim("private_keys", privateKeys);
        }
      }
      return builder.build();
    }
  }

}
