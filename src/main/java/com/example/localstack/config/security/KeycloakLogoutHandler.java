package com.example.localstack.config.security;

import com.example.localstack.constants.Constants;
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

/**
 * A logout handler responsible for managing the logout process involving Keycloak.
 * This class implements the Spring Security {@link LogoutHandler} interface and
 * triggers the appropriate logout actions when a user logs out of the application.
 *
 * The KeycloakLogoutHandler primarily ensures the integration with Keycloak by
 * invoking the Keycloak end-session endpoint during logout, ensuring that the
 * user is logged out both from the application and the Keycloak session.
 */
@Slf4j
@Component(value = "KeycloakLogoutHandler")
public class KeycloakLogoutHandler implements LogoutHandler {
    private final RestTemplate restTemplate;

    public KeycloakLogoutHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Logs out a user from the application and triggers associated logout actions.
     *
     * @param request the HTTP servlet request that contains the logout request information
     * @param response the HTTP servlet response to send any required logout feedback
     * @param authentication the authentication object representing the authenticated user's details
     */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        logout(request, authentication);
    }

    public void logout(HttpServletRequest request, Authentication authentication) {
        logoutFromKeycloak((OidcUser) authentication.getPrincipal());
    }

    private void logoutFromKeycloak(OidcUser user) {
        String endSessionUrl = user.getIssuer().toString().concat(Constants.LOGOUT_ENDPOINT);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(endSessionUrl).queryParam("id_token_hint", user.getIdToken().getTokenValue());
        ResponseEntity<String> logoutResponse = restTemplate.getForEntity(uriBuilder.toUriString(), String.class);
        if (logoutResponse.getStatusCode().is2xxSuccessful()) {
            log.info("Successfully logged out from Keycloak");
        } else {
            log.error("Error logging out from Keycloak");
        }
    }
}
