package com.igreja.system.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowLeaderToReadMembersButBlockMutationsAndUsersModule() throws Exception {
        mockMvc.perform(get("/api/members")
                        .with(user("leader").authorities(new SimpleGrantedAuthority("ROLE_LEADER"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("leader").authorities(new SimpleGrantedAuthority("ROLE_LEADER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users")
                        .with(user("leader").authorities(new SimpleGrantedAuthority("ROLE_LEADER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRestrictReservationMutationsByRole() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .with(user("member").authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(user("member").authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(user("leader").authorities(new SimpleGrantedAuthority("ROLE_LEADER"))))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/reservations/1/approve")
                        .with(user("leader").authorities(new SimpleGrantedAuthority("ROLE_LEADER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/reservations/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(user("leader").authorities(new SimpleGrantedAuthority("ROLE_LEADER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/reservations/1/approve")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldAllowAdminToManageProtectedModules() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isInternalServerError());
    }
}
