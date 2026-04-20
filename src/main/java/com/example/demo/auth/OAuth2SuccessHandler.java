package com.example.demo.auth;

import com.example.demo.entity.AppUser;
import com.example.demo.jwt.JwtService;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.service.AppUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.admin-email}")
    private String adminEmail;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        try {
            OAuth2User user = (OAuth2User) authentication.getPrincipal();

            String email = normalizeEmail(user.getAttribute("email"));
            if (email == null || email.isBlank()) {
                response.sendError(400, "Google did not provide email. Check scopes (openid,profile,email).");
                return;
            }

            String nameAttr = user.getAttribute("name");
            final String finalFullName =
                    (nameAttr == null || nameAttr.isBlank())
                            ? email.split("@")[0]
                            : nameAttr;

            AppUser dbUser = appUserRepository.findByEmail(email).orElseGet(() -> {
                AppUser u = new AppUser();
                u.setEmail(email);
                u.setName(finalFullName);
                u.setUsername(finalFullName);
                u.setPassword(passwordEncoder.encode("OAUTH2_USER"));

                if (isAdminEmail(email)) {
                    u.setRole("ADMIN");
                } else {
                    u.setRole("USER");
                }

                return appUserRepository.save(u);
            });

            if (isAdminEmail(email) && !"ADMIN".equalsIgnoreCase(dbUser.getRole())) {
                dbUser.setRole("ADMIN");
                appUserRepository.save(dbUser);
            }

            if (dbUser.getRole() == null || dbUser.getRole().isBlank()) {
                dbUser.setRole("USER");
                appUserRepository.save(dbUser);
            }

            if (dbUser.getPassword() == null || dbUser.getPassword().isBlank()) {
                dbUser.setPassword(passwordEncoder.encode("OAUTH2_USER"));
                appUserRepository.save(dbUser);
            }

            if (dbUser.getName() == null || dbUser.getName().isBlank()) {
                dbUser.setName(finalFullName);
                if (dbUser.getUsername() == null || dbUser.getUsername().isBlank()) {
                    dbUser.setUsername(finalFullName);
                }
                appUserRepository.save(dbUser);
            }

            String token = jwtService.generateToken(email);

            boolean isLocal = "localhost".equalsIgnoreCase(request.getServerName());

            String setCookie =
                    "access_token=" + token +
                            "; Path=/" +
                            "; Max-Age=" + (60 * 60) +
                            "; HttpOnly" +
                            (isLocal ? "" : "; Secure") +
                            "; SameSite=" + (isLocal ? "Lax" : "None");

            response.addHeader("Set-Cookie", setCookie);

            var userDetails = appUserService.loadUserByUsername(email);
            var appAuth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(appAuth);
            request.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );

            response.sendRedirect(frontendUrl + "/#/oauth-success");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "OAuth success error: " + e.getMessage());
        }
    }

    private boolean isAdminEmail(String email) {
        String configuredAdmin = normalizeEmail(adminEmail);
        return configuredAdmin != null && configuredAdmin.equals(normalizeEmail(email));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
