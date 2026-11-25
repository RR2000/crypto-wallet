package com.rondinella.authorizationserver.web;

import com.rondinella.authorizationserver.config.AuthorizationServerConfig;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDatabase;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/** Login and registration pages of the authorization server. */
@Controller
public class PageController {

  private final CryptoUserDetailsService users;
  private final String webUrl;
  private final boolean gitHub;

  PageController(CryptoUserDetailsService users, @Value("${wallet.web-url}") String webUrl,
                 AuthorizationServerConfig.GitHubLogin gitHub) {
    this.users = users;
    this.webUrl = webUrl;
    this.gitHub = gitHub.enabled();
  }

  @ModelAttribute
  void common(Model model) {
    model.addAttribute("webUrl", webUrl);
    model.addAttribute("gitHub", gitHub);
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/newUser")
  public String newUser(Model model) {
    model.addAttribute("cryptoUserDatabase", new CryptoUserDatabase());
    return "newUser";
  }

  @PostMapping("/addUser")
  public String addUser(@ModelAttribute CryptoUserDatabase user, Model model) {
    String error = users.register(user);
    if (error != null) {
      user.setPassword("");
      model.addAttribute("error", error);
      return "newUser";
    }
    return "redirect:/login?registered";
  }

  /** Nothing to see on the server itself: back to the web app. */
  @GetMapping("/")
  public String index() {
    return "redirect:" + webUrl + "/";
  }
}
