package com.rondinella.authorizationserver.controller;

import com.rondinella.authorizationserver.usermanaging.CryptoUserDatabase;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import com.rondinella.authorizationserver.usermanaging.CryptoUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

@Controller
public class DefaultController {

  CryptoUserPrincipal principal;
  CryptoUserDetailsService service;
  PasswordEncoder passwordEncoder;

  DefaultController(CryptoUserDetailsService service, PasswordEncoder passwordEncoder) {
    this.service = service;
    this.passwordEncoder = passwordEncoder;
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

  ///4c0883a69102937d6231471b5dbb6204fe5129617082792ae468d01a3f362318
  public static File encodeWallet(BigInteger privateKey, String password) throws IOException, CipherException {
    ECKeyPair keyPair = ECKeyPair.create(privateKey);
    File walletFile = File.createTempFile("wallet", ".json");
    String name = WalletUtils.generateWalletFile(password, keyPair, walletFile.getParentFile(), false);

    return new File(walletFile.getParentFile() + "/" + name);
  }


  @PostMapping("/addPrivateKey")
  public String addPrivateKey(Model model,
                              @RequestParam String private_key) throws CipherException, IOException {
    BigInteger privateKey = new BigInteger(private_key, 16);
    String password = "mypassword";

    File walletContents = encodeWallet(privateKey, password);
    System.out.println(FileUtils.readFileToString(walletContents, "UTF-8"));

    Credentials credentials = WalletUtils.loadCredentials(password, walletContents);
    System.out.printf("%x", credentials.getEcKeyPair().getPrivateKey());

    return index(model);
  }
}
