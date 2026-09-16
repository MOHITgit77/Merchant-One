package com.shopflow.user;

import com.shopflow.exception.DuplicateResourceException;
import com.shopflow.security.JwtTokenProvider;
import com.shopflow.user.dto.AuthResponse;
import com.shopflow.user.dto.LoginRequest;
import com.shopflow.user.dto.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.MERCHANT);

        user = userRepository.save(user);
        log.info("New merchant registered: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        log.info("Merchant logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    public AuthResponse.UserDto getUserProfile(java.util.UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.shopflow.exception.ResourceNotFoundException("User", "id", userId));
        return new AuthResponse.UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());
        AuthResponse.UserDto userDto = new AuthResponse.UserDto(
                user.getId(), user.getName(), user.getEmail(), user.getRole().name());
        return new AuthResponse(accessToken, refreshToken, userDto);
    }
}
