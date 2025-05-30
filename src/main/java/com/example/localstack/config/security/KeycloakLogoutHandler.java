package com.example.localstack.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component(value = "KeycloakLogoutHandler")
public class KeycloakLogoutHandler implements LogoutHandler {
    private final RestTemplate restTemplate;

    public KeycloakLogoutHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * @param request
     * @param response
     * @param authentication
     */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        logout(request, authentication);
    }

    public void logout(HttpServletRequest request, Authentication authentication) {
        logoutFromKeycloak((OidcUser) authentication.getPrincipal());
    }

    private void logoutFromKeycloak(OidcUser user) {
        String endSessionUrl = user.getIssuer().toString().concat("/protocol/openid-connect/logout");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(endSessionUrl).queryParam("id_token_hint", user.getIdToken().getTokenValue());
        ResponseEntity<String> logoutResponse = restTemplate.getForEntity(uriBuilder.toUriString(), String.class);
        if (logoutResponse.getStatusCode().is2xxSuccessful()) {
            log.info("Successfully logged out from Keycloak");
        } else {
            log.error("Error logging out from Keycloak");
        }
    }
}
