package com.rondinella.clientoauth.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class  DefaultController {

  @GetMapping("/")
  public String root() {
    return "redirect:/index";
  }

  @GetMapping("/index")
  public String index(Model model, @AuthenticationPrincipal OidcUser principal) {
    model.addAttribute("name", principal.getGivenName());
    model.addAttribute("wallets", principal.getClaim("wallets").toString());
    return "index";
  }
}
