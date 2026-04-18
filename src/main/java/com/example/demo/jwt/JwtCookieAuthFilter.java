package com.example.demo.jwt;

import com.example.demo.service.AppUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtCookieAuthFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "access_token";

    private final JwtService jwtService;
    private final AppUserService appUserService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // skip CORS preflight + keep old OAuth exclusions
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/login")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // allow CORS preflight through immediately
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractTokenFromCookie(request);

        // fallback: Authorization header Bearer
        if (token == null) {
            token = extractTokenFromHeader(request);
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String email;
        try {
            email = jwtService.extractEmail(token);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        if (email == null || email.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        UserDetails userDetails;
        try {
            userDetails = appUserService.loadUserByUsername(email);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtService.validateToken(token, userDetails)) {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie c : cookies) {
            if (COOKIE_NAME.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}