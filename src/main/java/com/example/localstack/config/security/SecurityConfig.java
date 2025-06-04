package com.example.localstack.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {
    private static final String ROLES_CLAIM = "roles";
    private static final String GROUPS = "groups";
    private static final String REALM_ACCESS_CLAIM = "realm_access";

    private final KeycloakLogoutHandler keycloakLogoutHandler;

    public SecurityConfig(KeycloakLogoutHandler keycloakLogoutHandler) {
        this.keycloakLogoutHandler = keycloakLogoutHandler;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(sessionRegistry());
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/api/v1/**"))
                .authenticated()
                .requestMatchers(new AntPathRequestMatcher("/api/v1/documents/**"))
                .hasRole("user")
                .requestMatchers(new AntPathRequestMatcher("/actuator/**"))
                .permitAll()
                .requestMatchers(new AntPathRequestMatcher("/"))
                .permitAll()
                .anyRequest()
                .authenticated());
        httpSecurity
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter());
                }));
        httpSecurity
                .oauth2Login(oauth2 -> oauth2.userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(userAuthoritiesMapperKeycloak())))
                .logout(logout -> logout.addLogoutHandler(keycloakLogoutHandler)).oidcLogout();
        return httpSecurity.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("realm_access.roles");
        converter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter authConverter = new JwtAuthenticationConverter();
        authConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = converter.convert(jwt);
            if (jwt.getClaim("realm_access") != null) {
                Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                if (realmAccess.containsKey("roles")) {
                    Collection<String> roles = (Collection<String>) realmAccess.get("roles");
                    log.info("Roles: {}", roles);
                    authorities.addAll(generateAuthoritiesFromClaims(roles));
                }
            }
            return authorities;
        });
        return authConverter;
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapperKeycloak() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuth = new HashSet<>();
            var authority = authorities.iterator().next();
            boolean isOidsType = authority instanceof OidcUserAuthority;
            if (isOidsType) {
                var oidcUserAuthority = (OidcUserAuthority) authority;
                var userInfo = oidcUserAuthority.getUserInfo();
                if (userInfo.hasClaim(REALM_ACCESS_CLAIM)) {
                    Map<String, Object> realmAccesses = userInfo.getClaimAsMap(REALM_ACCESS_CLAIM);
                    var roles = (Collection<String>) realmAccesses.get(ROLES_CLAIM);
                    mappedAuth.addAll(generateAuthoritiesFromClaims(roles));
                } else if (userInfo.hasClaim(GROUPS)) {
                    Collection<String> roles = userInfo.getClaim(GROUPS);
                    mappedAuth.addAll(generateAuthoritiesFromClaims(roles));
                }
            } else {
                var oauth2UserAuthority = (OAuth2UserAuthority) authority;
                Map<String, Object> attributes = oauth2UserAuthority.getAttributes();
                if (attributes.containsKey(REALM_ACCESS_CLAIM)) {
                    Map<String, Object> realmAccess = (Map<String, Object>) attributes.get(REALM_ACCESS_CLAIM);
                    Collection<String> roles = (Collection<String>) realmAccess.get(ROLES_CLAIM);
                    mappedAuth.addAll(generateAuthoritiesFromClaims(roles));
                }
            }
            return mappedAuth;
        };
    }

    Collection<SimpleGrantedAuthority> generateAuthoritiesFromClaims(Collection<String> roles) {
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_".concat(role))).toList();
    }
}
