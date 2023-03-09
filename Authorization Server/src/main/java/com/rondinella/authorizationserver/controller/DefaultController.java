package com.rondinella.authorizationserver.controller;

import com.rondinella.authorizationserver.usermanaging.CryptoUserDatabase;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import com.rondinella.authorizationserver.usermanaging.CryptoUserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
  public String index() {
    principal = (CryptoUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return "index";
  }

  @PostMapping(value = "/addUser")
  public String addUser(@ModelAttribute CryptoUserDatabase user) {
    if (user.getPassword().length() > 0 && user.getEmail().length() > 0) {
      user.setPassword(passwordEncoder.encode(user.getPassword()));
      user.addAuthority("USER");
      service.addCryptoUser(user);
    }
    return "login";
  }

  private String extractUsername(HttpServletRequest request) {
    String barer = request.getHeader("Authorization");
    String token = barer.split("Bearer ")[1];
    Jwt jwt = jwtDecoder.decode(token);
    Map<String, Object> claims = jwt.getClaims();
    return claims.get("sub").toString();
  }

  @PostMapping("/updateWallet")
  public ResponseEntity<String> addPrivateKey(@RequestParam String wallet,
                                              HttpServletRequest request) {

    String username = extractUsername(request);
    service.updateCryptoUserWallet(username, wallet);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  // it has to check the hash before changing pwd
  @PostMapping("/changePassword")
  public ResponseEntity<String> changePassword(@RequestParam String newPassword,
                                               HttpServletRequest request) {

    String username = extractUsername(request);
    service.updateCryptoUserPassword(username, newPassword);
    return new ResponseEntity<>(HttpStatus.OK);
  }


}
