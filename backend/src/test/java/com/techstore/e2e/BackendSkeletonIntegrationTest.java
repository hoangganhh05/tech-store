package com.techstore.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BackendSkeletonIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void applicationConnectsToTestDatabaseAndExposesOpenApi() {
        ResponseEntity<String> health = restTemplate.getForEntity("/api/v1/health", String.class);
        ResponseEntity<String> openApi = restTemplate.getForEntity("/api-docs", String.class);

        assertEquals(HttpStatus.OK, health.getStatusCode());
        assertNotNull(health.getBody());
        assertTrue(health.getBody().contains("\"database\":\"UP\""));

        assertEquals(HttpStatus.OK, openApi.getStatusCode());
        assertNotNull(openApi.getBody());
        assertTrue(openApi.getBody().contains("TechStore API"));
    }
}
