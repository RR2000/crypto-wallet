package com.rondinella.authorizationserver.web;

import com.rondinella.authorizationserver.config.AuthorizationServerConfig;
import com.rondinella.authorizationserver.mail.ClientIp;
import com.rondinella.authorizationserver.mail.PasswordResetService;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDatabase;
import com.rondinella.authorizationserver.usermanaging.CryptoUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/** Login, registration and "forgot password" pages of the authorization server. */
@Controller
public class PageController {

  private final CryptoUserDetailsService users;
  private final String webUrl;
  private final boolean gitHub;
  private final PasswordResetService passwordReset;

  PageController(CryptoUserDetailsService users, @Value("${wallet.web-url}") String webUrl,
                 AuthorizationServerConfig.GitHubLogin gitHub, PasswordResetService passwordReset) {
    this.users = users;
    this.webUrl = webUrl;
    this.gitHub = gitHub.enabled();
    this.passwordReset = passwordReset;
  }

  @ModelAttribute
  void common(Model model) {
    model.addAttribute("webUrl", webUrl);
    model.addAttribute("gitHub", gitHub);
    model.addAttribute("passwordReset", passwordReset.enabled());
  }

  /** What the web app may offer: "Sign in with GitHub" and "forgot password" by email. */
  @GetMapping(path = "/features", produces = "application/json")
  @ResponseBody
  public Map<String, Boolean> features() {
    return Map.of("github", gitHub, "passwordReset", passwordReset.enabled());
  }

  @GetMapping("/forgotPassword")
  public String forgotPassword() {
    requireReset();
    return "forgotPassword";
  }

  /** Same answer whether an account has this email or not. */
  @PostMapping("/forgotPassword")
  public String requestReset(@RequestParam(defaultValue = "") String email, HttpServletRequest request,
                             HttpServletResponse response, Model model) {
    requireReset();
    switch (passwordReset.requestReset(email, ClientIp.of(request))) {
      case INVALID_EMAIL -> {
        model.addAttribute("error", "Enter a valid email address.");
        return "forgotPassword";
      }
      case TOO_MANY -> {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        model.addAttribute("error", "Too many requests: try again later.");
        return "forgotPassword";
      }
      default -> {
        return "redirect:/forgotPassword?sent";
      }
    }
  }

  /** The page of the link in the email; the token is in the fragment, the page puts it in the form. */
  @GetMapping("/resetPassword")
  public String resetPasswordPage() {
    requireReset();
    return "resetPassword";
  }

  @PostMapping("/resetPassword")
  public String resetPassword(@RequestParam(defaultValue = "") String token,
                              @RequestParam(defaultValue = "") String password,
                              @RequestParam(defaultValue = "") String confirm, Model model) {
    requireReset();
    if (!password.equals(confirm)) {
      model.addAttribute("error", "The two passwords do not match.");
      return "resetPassword";
    }
    switch (passwordReset.resetPassword(token, password)) {
      case DONE -> {
        return "redirect:/login?reset";
      }
      case BAD_PASSWORD -> {
        model.addAttribute("error", "The password needs at least 8 characters (at most 72 bytes).");
        return "resetPassword";
      }
      default -> {
        model.addAttribute("badLink", true);
        return "resetPassword";
      }
    }
  }

  private void requireReset() {
    if (!passwordReset.enabled()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
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
