package com.optiroute.backend.service;

import com.optiroute.backend.dto.request.LoginRequest;
import com.optiroute.backend.dto.response.LoginResponse;
import com.optiroute.backend.entity.User;
import com.optiroute.backend.repository.UserRepository;
import com.optiroute.backend.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Hash factice comparé quand l'email est inconnu, pour que la durée de réponse ne révèle pas les comptes existants
    private final String unknownUserHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.unknownUserHash = passwordEncoder.encode("unknown-user-password");
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user == null ? unknownUserHash : user.getPassword());

        if (user == null || !passwordMatches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.");
        }

        String token = jwtService.generateToken(user.getEmail());

        return LoginResponse.builder().token(token).firstName(user.getFirstName()).lastName(user.getLastName()).build();
    }
}