package com.techstore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimitFilterTest {

    @Test
    void blocksAfterFiveFailedAttemptsPerSourceAndEndpoint() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(objectMapper());

        for (int i = 0; i < LoginRateLimitFilter.MAX_FAILED_ATTEMPTS; i++) {
            MockHttpServletResponse response = invoke(filter, "/api/v1/auth/login", (request, result) ->
                    ((HttpServletResponse) result).setStatus(HttpServletResponse.SC_UNAUTHORIZED));
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        }

        MockHttpServletResponse blocked = invoke(filter, "/api/v1/auth/login", (request, result) -> {
            throw new AssertionError("blocked requests must not reach the controller");
        });
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotBlank();
        assertThat(blocked.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void successfulLoginClearsPreviousFailures() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(objectMapper());
        for (int i = 0; i < 2; i++) {
        invoke(filter, "/api/v1/auth/login", (request, result) ->
                ((HttpServletResponse) result).setStatus(HttpServletResponse.SC_UNAUTHORIZED));
        }
        invoke(filter, "/api/v1/auth/login", (request, result) ->
                ((HttpServletResponse) result).setStatus(HttpServletResponse.SC_OK));

        for (int i = 0; i < LoginRateLimitFilter.MAX_FAILED_ATTEMPTS - 1; i++) {
            invoke(filter, "/api/v1/auth/login", (request, result) ->
                    ((HttpServletResponse) result).setStatus(HttpServletResponse.SC_UNAUTHORIZED));
        }
        MockHttpServletResponse next = invoke(filter, "/api/v1/auth/login", (request, result) ->
                ((HttpServletResponse) result).setStatus(HttpServletResponse.SC_OK));
        assertThat(next.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    private MockHttpServletResponse invoke(
            LoginRateLimitFilter filter,
            String path,
            jakarta.servlet.FilterChain chain
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr("192.0.2.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
