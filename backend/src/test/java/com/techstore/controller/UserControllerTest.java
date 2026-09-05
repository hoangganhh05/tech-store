package com.techstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.request.UpdateProfileRequest;
import com.techstore.dto.response.UserProfileResponse;
import com.techstore.security.AccessTokenAuthenticator;
import com.techstore.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccessTokenAuthenticator accessTokenAuthenticator;

    @MockitoBean
    private UserProfileService userProfileService;

    @Test
    void getMeReturnsTheAuthenticatedUserProfile() throws Exception {
        when(accessTokenAuthenticator.authenticate("Bearer access-token")).thenReturn(1L);
        when(userProfileService.getProfile(1L)).thenReturn(profile());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("customer@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.data.dateOfBirth").value("2000-05-20"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void updateMeValidatesFieldsBeforeCallingTheService() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"phone\":\"abc\",\"dateOfBirth\":\"2999-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("fullName"),
                        org.hamcrest.Matchers.containsString("phone"),
                        org.hamcrest.Matchers.containsString("dateOfBirth"))));
    }

    @Test
    void updateMeAcceptsOnlyEditableProfileFields() throws Exception {
        when(accessTokenAuthenticator.authenticate("Bearer access-token")).thenReturn(1L);
        when(userProfileService.updateProfile(any(), any(UpdateProfileRequest.class))).thenReturn(profile());

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Nguyen Van A\",\"phone\":\"0901234567\",\"dateOfBirth\":\"2000-05-20\",\"email\":\"attacker@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật thông tin cá nhân thành công"))
                .andExpect(jsonPath("$.data.email").value("customer@example.com"));

        verify(userProfileService).updateProfile(any(), any(UpdateProfileRequest.class));
    }

    private UserProfileResponse profile() {
        return new UserProfileResponse(
                1L,
                "customer@example.com",
                "Nguyen Van A",
                "0901234567",
                LocalDate.of(2000, 5, 20),
                Instant.parse("2026-09-05T08:00:00Z")
        );
    }
}
