package org.shrtr.core.controllers;

import com.sun.istack.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.shrtr.core.config.security.JwtTokenUtil;
import org.shrtr.core.domain.entities.User;
import org.shrtr.core.services.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.Set;

@RestController
@RequestMapping("/authentication")
@RequiredArgsConstructor
public class AuthenticationController {
  private final AuthenticationManager authenticationManager;
  private final JwtTokenUtil jwtTokenUtil;
  private final UserService userService;

  @PostMapping("login")
  public ResponseEntity<User> login(@RequestBody @Valid AuthRequest request) {
    try {
      Authentication authenticate = authenticationManager
              .authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

      User user = (User) authenticate.getPrincipal();

      return ResponseEntity.ok()
              .header(HttpHeaders.AUTHORIZATION, jwtTokenUtil.generateAccessToken(user))
              .body(user);
    } catch (BadCredentialsException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  @PostMapping("register")
  public User register(@RequestBody @Valid CreateUserRequest request) {
    return userService.create(request);
  }

  @Data
  public static class AuthRequest {

    @NotNull
    @Email
    private String username;
    @NotNull
    private String password;

  }

  @Data
  public static class CreateUserRequest {

    @NotBlank
    @Email
    private String username;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    private String password;
    @NotBlank
    private String rePassword;

  }

}
