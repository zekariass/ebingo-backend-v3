//package com.ebingo.backend.system.config;
//
//import com.ebingo.backend.user.entity.UserProfile;
//import com.ebingo.backend.user.repository.UserProfileRepository;
//import org.springframework.data.domain.ReactiveAuditorAware;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.security.core.context.ReactiveSecurityContextHolder;
//import org.springframework.stereotype.Component;
//import reactor.core.publisher.Mono;
//
//import java.util.UUID;
//
//@Component("auditorAware")
//public class UserProfileAuditorAware implements ReactiveAuditorAware<Long> {
//
//    private final UserProfileRepository userProfileRepository;
//
//    public UserProfileAuditorAware(UserProfileRepository userProfileRepository) {
//        this.userProfileRepository = userProfileRepository;
//    }
//
//    @Override
//    public Mono<Long> getCurrentAuditor() {
//        return ReactiveSecurityContextHolder.getContext()
//                .map(securityContext -> securityContext.getAuthentication())
//                .filter(Authentication::isAuthenticated)
//                .flatMap(auth -> {
//                    if (auth.getPrincipal() instanceof Jwt jwt) {
//                        try {
//                            UUID uuid = UUID.fromString(jwt.getSubject());
//                            return userProfileRepository.findBySupabaseId(uuid)
//                                    .map(userProfile -> userProfile.getId());
//                        } catch (IllegalArgumentException e) {
//                            return Mono.empty();
//                        }
//                    }
//                    return Mono.empty();
//                });
//    }
//}
//
