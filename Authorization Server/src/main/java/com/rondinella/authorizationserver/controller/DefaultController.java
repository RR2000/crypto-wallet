package com.rondinella.authorizationserver.controller;

import com.rondinella.authorizationserver.usermanaging.CryptoUserDatabase;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import com.rondinella.authorizationserver.usermanaging.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Controller
public class DefaultController {

  CryptoUserDetailsService service;

  DefaultController(CryptoUserDetailsService service){
    this.service = service;
  }

  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("cryptoUserDatabase", new CryptoUserDatabase());
    return "index";
  }

  @PostMapping(value = "/addUser")
  public String addUser(@ModelAttribute CryptoUserDatabase user) {
    //TODO mettere crittografia
    user.setPassword("{noop}"+user.getPassword());
    user.addAuthority("USER");
    user.addWallet("fbdshjfversguarhgrekgvergreuyittygewoat747avwevfnret");
    service.addCryptoUser(user);
    return "index";
  }
}
