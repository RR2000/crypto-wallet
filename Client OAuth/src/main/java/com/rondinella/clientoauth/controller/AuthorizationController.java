package com.rondinella.clientoauth.controller;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthorizationController {

  @GetMapping("/authorized")
  public String addMessage(Model model,
                           @RegisteredOAuth2AuthorizedClient("crypto-wallet-client") OAuth2AuthorizedClient authorizedClient) {


    return "index";
  }
}
