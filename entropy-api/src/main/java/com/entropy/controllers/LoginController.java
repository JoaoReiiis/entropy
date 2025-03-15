package com.entropy.controllers;

import com.entropy.model.Users;
import com.entropy.model.dto.auth.AuthDTO;
import com.entropy.model.dto.auth.TokenDTO;
import com.entropy.security.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @PostMapping
    public ResponseEntity<TokenDTO> login(@RequestBody @Valid AuthDTO dto){
        var loginPassword = new UsernamePasswordAuthenticationToken(dto.login(), dto.password());
        var auth = this.authenticationManager.authenticate(loginPassword);

        String token = tokenService.gerarToken((Users) auth.getPrincipal());
        return ResponseEntity.ok(new TokenDTO(token));
    }
}
