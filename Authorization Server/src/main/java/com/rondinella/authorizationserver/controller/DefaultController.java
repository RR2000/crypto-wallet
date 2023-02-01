package com.rondinella.authorizationserver.controller;

import com.rondinella.authorizationserver.usermanaging.CryptoUserDatabase;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import com.rondinella.authorizationserver.usermanaging.CryptoUserPrincipal;
import com.rondinella.authorizationserver.usermanaging.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DefaultController {

  CryptoUserPrincipal principal;
  CryptoUserDetailsService service;

  DefaultController(CryptoUserDetailsService service){
    this.service = service;
  }

  @GetMapping("/newUser")
  public String newUser(Model model) {
	  model.addAttribute("cryptoUserDatabase", new CryptoUserDatabase());
	  return "newUser";
  }
  
  @GetMapping("/login")
  public String login(){
	  return "login";
  }
  
  @GetMapping("/")
  public String index(Model model) {
    principal = (CryptoUserPrincipal)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return "index";
  }

  @PostMapping(value = "/addUser")
  public String addUser(Model model, @ModelAttribute CryptoUserDatabase user) {
    if(user.getPassword().length() > 0 && user.getEmail().length() > 0) {
      //TODO mettere crittografia
      user.setPassword("{noop}" + user.getPassword());
      user.addAuthority("USER");
      service.addCryptoUser(user);
    }
    return "login";
  }

  @PostMapping( "/addPrivateKey")
  public String addPrivateKey(Model model,
                              @RequestParam String private_key) {
    service.updateCryptoUser(principal.getUsername(), private_key);
    return index(model);
  }
}
