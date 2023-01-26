package com.rondinella.clientoauth.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class  DefaultController {

  @GetMapping("/")
  public String root() {
    return "redirect:/index";
  }

  @GetMapping("/index")
  public String index(@AuthenticationPrincipal OidcUser principal) {
    System.out.println("my_user: " + principal.getUserInfo().getFullName());
    return "index";
  }
}
