//package com.ebingo.backend.system.converters;
/// /
/// /import com.ebingo.backend.user.service.UserProfileService;
/// /import lombok.RequiredArgsConstructor;
/// /import lombok.extern.slf4j.Slf4j;
/// /import org.springframework.core.convert.converter.Converter;
/// /import org.springframework.security.authentication.AbstractAuthenticationToken;
/// /import org.springframework.security.core.authority.SimpleGrantedAuthority;
/// /import org.springframework.security.oauth2.jwt.Jwt;
/// /import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
/// /import org.springframework.stereotype.Component;
/// /
/// /import java.util.Collection;
/// /import java.util.HashSet;
/// /import java.util.Map;
/// /import java.util.Set;
/// /
/// /@Component
/// /@Slf4j
/// /@RequiredArgsConstructor
/// /public class ReactiveCustomRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {
/// /
/// /
/// /    private final UserProfileService userProfileService;
/// /    private static final String ROLE_PREFIX = "ROLE_";
/// /
/// /
/// /    @Override
/// /    public AbstractAuthenticationToken convert(Jwt jwt) {
/// /        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
/// /
/// /        // JWT "role"
/// /        String role = jwt.getClaimAsString("role");
/// /        if (role != null) authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
/// /
/// /        // JWT "roles"
/// /        Object rolesClaim = jwt.getClaim("roles");
/// /        if (rolesClaim instanceof Collection) {
/// /            ((Collection<?>) rolesClaim).forEach(r -> {
/// /                if (r != null) authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()));
/// /            });
/// /        }
/// /
/// /        // JWT permissions
/// /        Object permissions = jwt.getClaim("permissions");
/// /        if (permissions instanceof Map) {
/// /            ((Map<?, ?>) permissions).forEach((k, v) -> {
/// /                if (v instanceof Boolean && (Boolean) v) {
/// /                    authorities.add(new SimpleGrantedAuthority("PERM_" + k.toString().toUpperCase()));
/// /                }
/// /            });
/// /        }
/// /
/// /
/// /        // fallback
/// /        if (authorities.isEmpty() && jwt.getClaimAsString("email") != null) {
/// /            authorities.add(new SimpleGrantedAuthority("ROLE_PLAYER"));
/// /        }
/// /
/// /        return new JwtAuthenticationToken(jwt, authorities);
/// /    }
/// /}
//
////package com.ebingo.backend.system.converters;
//
//import com.ebingo.backend.user.service.UserProfileService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.convert.converter.Converter;
//import org.springframework.security.authentication.AbstractAuthenticationToken;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
//import org.springframework.stereotype.Component;
//import reactor.core.publisher.Mono;
//
//import java.util.*;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class ReactiveCustomRoleConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {
//
//    private final UserProfileService userProfileService;
//    private static final String ROLE_PREFIX = "ROLE_";
//
//    @Override
//    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
//        // Extract roles from JWT
//        Set<GrantedAuthority> authorities = extractJwtAuthorities(jwt);
//
//        // Get user ID from "sub" claim
//        String sub = jwt.getClaimAsString("sub");
//        if (sub == null) {
//            return Mono.just(new JwtAuthenticationToken(jwt, authorities));
//        }
//
//        UUID userId;
//        try {
//            userId = UUID.fromString(sub);
//        } catch (IllegalArgumentException e) {
//            log.error("Invalid UUID in JWT 'sub' claim: {}", sub, e);
//            return Mono.just(new JwtAuthenticationToken(jwt, authorities));
//        }
//
//        // Fetch role from local DB reactively and merge with JWT roles
//        return userProfileService.getUserProfileBySupabaseId(userId)
//                .map(profile -> {
////                    log.info("===============================>>>> AUTHORITIES ===================>>>>: {}", profile.getRole().name().toUpperCase());
//
//                    Set<GrantedAuthority> merged = new HashSet<>(authorities);
//                    merged.add(new SimpleGrantedAuthority(ROLE_PREFIX + profile.getRole().name().toUpperCase()));
//                    return (AbstractAuthenticationToken) new JwtAuthenticationToken(jwt, merged);
//                })
//                .onErrorResume(e -> {
//                    log.error("Failed to fetch user profile: {}", e.getMessage(), e);
//                    return Mono.just(new JwtAuthenticationToken(jwt, authorities));
//                });
//    }
//
//    private Set<GrantedAuthority> extractJwtAuthorities(Jwt jwt) {
//        Set<GrantedAuthority> authorities = new HashSet<>();
//
//        // single role claim
//        String role = jwt.getClaimAsString("role");
//        if (role != null) {
//            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()));
//        }
//
//        // roles array
//        Object rolesClaim = jwt.getClaim("roles");
//        if (rolesClaim instanceof Collection<?>) {
//            ((Collection<?>) rolesClaim).forEach(r -> {
//                if (r != null) {
//                    authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + r.toString().toUpperCase()));
//                }
//            });
//        }
//
//        // permissions map
//        Object permissions = jwt.getClaim("permissions");
//        if (permissions instanceof Map<?, ?> map) {
//            map.forEach((k, v) -> {
//                if (v instanceof Boolean && (Boolean) v) {
//                    authorities.add(new SimpleGrantedAuthority("PERM_" + k.toString().toUpperCase()));
//                }
//            });
//        }
//
//        // fallback
//        if (authorities.isEmpty() && jwt.getClaimAsString("email") != null) {
//            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + "PLAYER"));
//        }
//
//
////        log.info("===============================>>>> AUTHORITIES ==================>>>>: {}", authorities);
//
//        return authorities;
//    }
//}
