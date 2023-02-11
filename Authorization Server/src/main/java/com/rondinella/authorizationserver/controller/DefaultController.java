package com.rondinella.authorizationserver.controller;

import com.rondinella.authorizationserver.usermanaging.CryptoUserDatabase;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import com.rondinella.authorizationserver.usermanaging.CryptoUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class DefaultController {

  CryptoUserPrincipal principal;
  CryptoUserDetailsService service;
  PasswordEncoder passwordEncoder;
  JwtDecoder jwtDecoder;

  DefaultController(CryptoUserDetailsService service, PasswordEncoder passwordEncoder, JwtDecoder jwtDecoder) {
    this.service = service;
    this.passwordEncoder = passwordEncoder;
    this.jwtDecoder = jwtDecoder;
  }

  @GetMapping("/newUser")
  public String newUser(Model model) {
    model.addAttribute("cryptoUserDatabase", new CryptoUserDatabase());
    return "newUser";
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/")
  public String index(Model model) {
    principal = (CryptoUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return "index";
  }

  @PostMapping(value = "/addUser")
  public String addUser(Model model, @ModelAttribute CryptoUserDatabase user) {
    if (user.getPassword().length() > 0 && user.getEmail().length() > 0) {
      user.setPassword(passwordEncoder.encode(user.getPassword()));
      user.addAuthority("USER");
      service.addCryptoUser(user);
    }
    return "login";
  }

  @PostMapping("/addPrivateKey")
  public ResponseEntity<String> addPrivateKey(Model model,
                                              @RequestParam String private_key,
                                              HttpServletRequest request) {

    String barer = request.getHeader("Authorization");
    String token = barer.split("Bearer ")[1];
    Jwt jwt = jwtDecoder.decode(token);
    Map<String, Object> claims = jwt.getClaims();
    String username = claims.get("sub").toString();
    service.updateCryptoUser(username, private_key);

    return new ResponseEntity<>(HttpStatus.CREATED);
  }
}
