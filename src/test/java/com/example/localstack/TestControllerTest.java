package com.example.localstack;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

import static org.junit.Assert.assertEquals;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@RunWith(SpringRunner.class)
public class TestControllerTest {
    private TestRestTemplate restTemplate;
    @Autowired
    private RestTemplateBuilder testRestTemplate;

    @TestConfiguration
    static class TestRestTemplateConfiguration {
        @Bean
        public RestTemplateBuilder testRestTemplate() {
            return new RestTemplateBuilder().rootUri("http://localhost:8081/");
        }
    }

    @Before
    public void setUp() {
        restTemplate = new TestRestTemplate(testRestTemplate);
    }

    private static final String KEYCLOAK_URL = "http://localhost:9090/realms/test_local_realm/protocol/openid-connect/token";
    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String CLIENT_ID;
    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String CLIENT_SECRET = "";
    private final String USERNAME = "test_user";
    private final String PASSWORD = "test";

    @Test
    public void testAuthentication() {
        String token = getKeycloakToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Object> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/test", HttpMethod.GET, entity, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Secured test API", response.getBody());
    }

    private String getKeycloakToken() {
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", CLIENT_ID);
        params.add("client_secret", CLIENT_SECRET);
        params.add("grant_type", "password");
        params.add("username", USERNAME);
        params.add("password", PASSWORD);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(KEYCLOAK_URL, request, Map.class);
        Assert.assertNotNull(response.getBody());

        return response.getBody().get("access_token").toString();
    }

}
